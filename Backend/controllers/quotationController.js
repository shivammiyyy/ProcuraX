import Quotation from "../models/quotationModel.js";
import Rfq from "../models/RfqModel.js";
import { predictVendorScore } from "../utils/complianceScore.js";
import User from "../models/userModel.js";
import { sendEmail } from "../utils/mailer.js";

export const createQuotation = async (req, res) => {
  try {
    const { rfqId, price, deliveryTimeDays, compliance } = req.body;
    const vendorId = req.user._id;
    const attachments = req.files ? req.files.map(file => ({
      fileName: file.originalname,
      filePath: file.path,
    })) : [];

    if (!rfqId || !price || !deliveryTimeDays || !compliance) {
      return res.status(400).json({ message: 'Please enter all required fields for the quotation.' });
    }

    const rfq = await Rfq.findById(rfqId);
    if (!rfq) {
      return res.status(404).json({ message: 'RFQ not found.' });
    }
    const priceDifference = rfq.budget - price;
    const complianceScore = (compliance.ISO_Certification ? 40 : 0) +
      (['A+'].includes(compliance.Material_Grade) ? 30 : ['A'].includes(compliance.Material_Grade) ? 20 : ['B'].includes(compliance.Material_Grade) ? 10 : 0) +
      (compliance.Environmental_Standards ? 20 : 0) +
      (compliance.Document_Submission ? 10 : 0);

    const vendorScore = await predictVendorScore([priceDifference, deliveryTimeDays, complianceScore]);

    const newQuotation = new Quotation({
      rfq: rfqId,
      vendor: vendorId,
      price,
      deliveryTimeDays,
      compliance: JSON.parse(compliance),
      vendorScore,
      attachments,
    });

    await newQuotation.save();
    res.status(201).json({ message: 'Quotation created successfully.', quotation: newQuotation });
  } catch (error) {
    console.error('Error creating quotation:', error);
    res.status(500).json({ message: 'Internal server error.' });
  }
};

export const getQuotations = async (req, res) => {
  try {
    let query = {};
    if (req.user.role === 'vendor') {
      query = { vendor: req.user._id };
    } else if (req.user.role === 'buyer') {
      const rfqs = await Rfq.find({ Buyer: req.user._id }).select('_id');
      const rfqIds = rfqs.map(rfq => rfq._id);
      query = { rfq: { $in: rfqIds } };
    }

    const quotations = await Quotation.find(query)
      .populate('rfq', 'title description budget status')
      .populate('vendor', 'fullName companyName email budget');

    res.status(200).json({ quotations });
  } catch (error) {
    console.error('Error fetching quotations:', error);
    res.status(500).json({ message: 'Failed to fetch quotations' });
  }
};

export const getQuotationById = async (req, res) => {
  try {
    const quotation = await Quotation.findById(req.params.id)
      .populate('rfq', 'title description budget status Buyer attachments')
      .populate('vendor', 'fullName companyName email attachments');

    if (!quotation) {
      return res.status(404).json({ message: 'Quotation not found' });
    }

    if (req.user.role === 'vendor' && quotation.vendor._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to view this quotation' });
    }
    if (req.user.role === 'buyer' && quotation.rfq.Buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to view this quotation' });
    }

    res.status(200).json({ quotation });
  } catch (error) {
    console.error('Error fetching quotation by ID:', error);
    res.status(500).json({ message: 'Failed to fetch quotation' });
  }
};

export const updateQuotation = async (req, res) => {
  const { price, deliveryTimeDays, compliance, status } = req.body;
  const attachments = req.files ? req.files.map(file => ({
    fileName: file.originalname,
    filePath: file.path,
  })) : [];

  try {
    const quotation = await Quotation.findById(req.params.id).populate('rfq');

    if (!quotation) {
      return res.status(404).json({ message: 'Quotation not found' });
    }

    if (req.user.role === 'vendor' && quotation.vendor.toString() === req.user._id.toString()) {
      if (quotation.rfq.status !== 'open') {
        return res.status(400).json({ message: 'Cannot update quotation for a closed or in-progress RFQ.' });
      }
      quotation.price = price || quotation.price;
      quotation.deliveryTimeDays = deliveryTimeDays || quotation.deliveryTimeDays;
      quotation.compliance = compliance ? JSON.parse(compliance) : quotation.compliance;
      if (attachments.length > 0) {
        quotation.attachments = [...quotation.attachments, ...attachments];
      }
    }
    else if (req.user.role === 'buyer' && quotation.rfq.Buyer.toString() === req.user._id.toString()) {
      if (status && ['accepted', 'rejected', 'under_review'].includes(status)) {
        quotation.status = status;
        if (status === 'accepted') {
          quotation.rfq.status = 'in_progress';
          await quotation.rfq.save();
          const vendor = await User.findById(quotation.vendor);
          if (vendor) {
            await sendEmail(
              vendor.email,
              `Your Quotation for ${quotation.rfq.title} Accepted!`,
              `<p>Your quotation for RFQ "<b>${quotation.rfq.title}</b>" has been accepted.</p><p>Proceed to contract finalization.</p>`
            );
          }
        } else if (status === 'rejected') {
          const vendor = await User.findById(quotation.vendor);
          if (vendor) {
            await sendEmail(
              vendor.email,
              `Your Quotation for ${quotation.rfq.title} Rejected`,
              `<p>Your quotation for RFQ "<b>${quotation.rfq.title}</b>" was not selected.</p><p>Thank you for your submission.</p>`
            );
          }
        }
      } else {
        return res.status(403).json({ message: 'Buyers can only update quotation status.' });
      }
    } else {
      return res.status(403).json({ message: 'Not authorized to update this quotation' });
    }

    const updatedQuotation = await quotation.save();
    res.status(200).json({ message: 'Quotation updated successfully', quotation: updatedQuotation });
  } catch (error) {
    console.error('Error updating quotation:', error);
    res.status(500).json({ message: 'Failed to update quotation' });
  }
};

export const deleteQuotation = async (req, res) => {
  try {
    const quotation = await Quotation.findById(req.params.id).populate('rfq');

    if (!quotation) {
      return res.status(404).json({ message: 'Quotation not found' });
    }

    if (quotation.vendor.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to delete this quotation' });
    }

    if (quotation.rfq.status !== 'open') {
      return res.status(400).json({ message: 'Cannot delete quotation as the RFQ is no longer open.' });
    }

    await Quotation.deleteOne({ _id: req.params.id });
    res.status(200).json({ message: 'Quotation deleted successfully' });
  } catch (error) {
    console.error('Error deleting quotation:', error);
    res.status(500).json({ message: 'Failed to delete quotation' });
  }
};
