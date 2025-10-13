import Quotation from "../models/quotationModel.js";
import Rfq from "../models/RfqModel.js";
import { predictVendorScore } from "../utils/complianceScore.js";
import User from "../models/userModel.js";
import sendEmail from "../utils/mailer.js";


export const createQuotation = async (req, res) => {
  try {
    const { rfqId, price, deliveryTimeDays, compliance } = req.body;
    const vendorId = req.user.id; // Assuming user ID is available in req.user
    const attachments = req.files ? req.files.map(file => ({
      fileName: file.originalname,
      filePath: file.path, // Cloudinary URL
    })) : [];


    if (!rfqId || !price || !deliveryTimeDays || !compliance) {
      return res.status(400).json({ message: 'Please enter all required fields for the quotation.' });
    }

    const rfq = await Rfq.findById(rfqId);
    if (!rfq) {
      return res.status(404).json({ message: 'RFQ not found.' });
    }
    const pricedifference = rfq.budget - price;
    const complianceScore = (compliance.ISO_Certification ? 40 : 0) +
      (['A+'].includes(compliance.Material_Grade) ? 30 : ['A'].includes(compliance.Material_Grade) ? 20 : ['B'].includes(compliance.Material_Grade) ? 10 : 0) +
      (compliance.Environmental_Standards ? 20 : 0) +
      (compliance.Document_Submission ? 10 : 0);


    const vendorScore = await predictVendorScore([pricedifference, deliveryTimeDays, complianceScore]);
    console.log('Predicted Vendor Score:', vendorScore);

    const newQuotation = new Quotation({
      rfq: rfqId,
      vendor: vendorId,
      price,
      deliveryTimeDays,
      compliance: JSON.parse(compliance),
      vendorScore,
      attachments: attachments,
    });

    await newQuotation.save();
    res.status(201).json({ message: 'Quotation created successfully.', quotation: newQuotation });
  } catch (error) {
    console.error('❌ Error creating quotation:', error);
    res.status(500).json({ message: 'Internal server error.' });
  }
};

// @desc    Get all quotations (filtered by role)
// @route   GET /api/v0/quotation
// @access  Private/Admin/Buyer/Vendor
export const getQuotations = async (req, res) => {
  try {
    let query = {};
    if (req.user.role === 'vendor') {
      query = { vendor: req.user._id }; // Vendors only see their own quotations
    } else if (req.user.role === 'buyer') {
      // Buyers need quotations for RFQs they created
      const rfqs = await Rfq.find({ Buyer: req.user._id }).select('_id');
      const rfqIds = rfqs.map(rfq => rfq._id);
      query = { rfq: { $in: rfqIds } };
    }
    // Admin can see all, no query needed

    const quotations = await Quotation.find(query)
      .populate('rfq', 'title description budget status')
      .populate('vendor', 'fullName companyName email');

    res.status(200).json({ quotations });
  } catch (error) {
    console.error('Error fetching quotations:', error);
    res.status(500).json({ message: 'Failed to fetch quotations', error: error.message });
  }
};

// @desc    Get a single quotation by ID
// @route   GET /api/v0/quotation/:id
// @access  Private/Admin/Buyer/Vendor
export const getQuotationById = async (req, res) => {
  try {
    const quotation = await Quotation.findById(req.params.id)
      .populate('rfq', 'title description budget status Buyer')
      .populate('vendor', 'fullName companyName email');

    if (!quotation) {
      return res.status(404).json({ message: 'Quotation not found' });
    }

    // Authorization: Vendor can only see their own. Buyer can only see for their RFQs.
    if (req.user.role === 'vendor' && quotation.vendor._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to view this quotation' });
    }
    if (req.user.role === 'buyer' && quotation.rfq.Buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to view this quotation' });
    }

    res.status(200).json({ quotation });
  } catch (error) {
    console.error('Error fetching quotation by ID:', error);
    res.status(500).json({ message: 'Failed to fetch quotation', error: error.message });
  }
};


// @desc    Update a quotation (only by the vendor who submitted it, if RFQ is open; or buyer to accept/reject)
// @route   PUT /api/v0/quotation/:id
// @access  Private/Vendor or Private/Buyer
export const updateQuotation = async (req, res) => {
  const { price, deliveryTimeDays, compliance, status } = req.body; // Status update mainly for buyer to accept/reject
  const attachments = req.files ? req.files.map(file => ({
    fileName: file.originalname,
    filePath: file.path,
  })) : [];

  try {
    const quotation = await Quotation.findById(req.params.id).populate('rfq');

    if (!quotation) {
      return res.status(404).json({ message: 'Quotation not found' });
    }

    // Vendor can update their own quotation if RFQ is open
    if (req.user.role === 'vendor' && quotation.vendor.toString() === req.user._id.toString()) {
      if (quotation.rfq.status !== 'open') {
        return res.status(400).json({ message: 'Cannot update quotation for a closed or in-progress RFQ.' });
      }
      quotation.price = price || quotation.price;
      quotation.deliveryTimeDays = deliveryTimeDays || quotation.deliveryTimeDays;
      quotation.compliance = compliance ? JSON.parse(compliance) : quotation.compliance;
      // Append new attachments
      if (attachments.length > 0) {
        quotation.attachments = [...quotation.attachments, ...attachments];
      }
    }
    // Buyer can update the status of a quotation (e.g., accept/reject)
    else if (req.user.role === 'buyer' && quotation.rfq.Buyer.toString() === req.user._id.toString()) {
      if (status && ['accepted', 'rejected', 'under_review'].includes(status)) {
        quotation.status = status;
        // If accepted, update RFQ status to in_progress or closed as appropriate
        if (status === 'accepted') {
          quotation.rfq.status = 'in_progress'; // Or 'closed' if only one winner
          await quotation.rfq.save();
          // Notify vendor of acceptance
          const vendor = await User.findById(quotation.vendor);
          if (vendor) {
            await sendEmail(
              vendor.email,
              `Your Quotation for ${quotation.rfq.title} Accepted!`,
              `<p>Good news! Your quotation for RFQ "<b>${quotation.rfq.title}</b>" has been accepted by the buyer.</p>
                         <p>Proceed to contract finalization.</p>`
            );
          }
        } else if (status === 'rejected') {
          // Notify vendor of rejection
          const vendor = await User.findById(quotation.vendor);
          if (vendor) {
            await sendEmail(
              vendor.email,
              `Your Quotation for ${quotation.rfq.title} Rejected`,
              `<p>Your quotation for RFQ "<b>${quotation.rfq.title}</b>" was unfortunately not selected at this time.</p>
                          <p>Thank you for your submission.</p>`
            );
          }
        }
      } else {
        return res.status(403).json({ message: 'Buyers can only update quotation status.' });
      }
    }
    else {
      return res.status(403).json({ message: 'Not authorized to update this quotation' });
    }

    const updatedQuotation = await quotation.save();
    res.status(200).json({ message: 'Quotation updated successfully', quotation: updatedQuotation });
  } catch (error) {
    console.error('Error updating quotation:', error);
    res.status(500).json({ message: 'Failed to update quotation', error: error.message });
  }
};

// @desc    Delete a quotation (only by the vendor who submitted it, if RFQ is open)
// @route   DELETE /api/v0/quotation/:id
// @access  Private/Vendor
export const deleteQuotation = async (req, res) => {
  try {
    const quotation = await Quotation.findById(req.params.id).populate('rfq');

    if (!quotation) {
      return res.status(404).json({ message: 'Quotation not found' });
    }

    // Check if the authenticated user is the vendor who submitted the quotation
    if (quotation.vendor.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to delete this quotation' });
    }

    // A vendor can only delete their quotation if the associated RFQ is still 'open'
    if (quotation.rfq.status !== 'open') {
      return res.status(400).json({ message: 'Cannot delete quotation as the associated RFQ is no longer open.' });
    }

    await Quotation.deleteOne({ _id: req.params.id });
    res.status(200).json({ message: 'Quotation deleted successfully' });
  } catch (error) {
    console.error('Error deleting quotation:', error);
    res.status(500).json({ message: 'Failed to delete quotation', error: error.message });
  }
};