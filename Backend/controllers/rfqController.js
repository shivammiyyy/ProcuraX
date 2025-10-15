import User from '../models/userModel.js';
import Rfq from '../models/RfqModel.js';
import { sendEmail } from '../utils/mailer.js';
import Quotation from '../models/quotationModel.js';

export const createRfq = async (req, res) => {
  const { title, description, requestType, budget, deadline, category } = req.body;
  const attachments = req.files?.length
    ? req.files.map(file => ({
      fileName: file.originalname,
      filePath: file.path,
    }))
    : [];

  if (!title || !description || !requestType || !budget || !deadline || !category) {
    return res.status(400).json({ message: 'Please enter all required fields for the RFQ.' });
  }
  try {
    const newRfq = await Rfq.create({
      title,
      description,
      requestType,
      budget,
      deadline,
      category,
      Buyer: req.user._id,
      attachments,
    });
    await newRfq.save();

    const vendors = await User.find({ role: 'vendor' }).select('email');
    if (vendors.length > 0) {
      const vendorEmails = vendors.map(vendor => vendor.email);
      await sendEmail(
        vendorEmails.join(','),
        `New ${requestType} Posted: ${title}`,
        `<p>A new ${requestType} titled "<b>${title}</b>" has been posted. Deadline: ${new Date(deadline).toLocaleDateString()}</p><p>Description: ${description}</p>`
      );
    }
    res.status(201).json({ message: 'RFQ created successfully', rfq: newRfq });
  } catch (error) {
    console.error('Error creating RFQ:', error);
    res.status(500).json({ message: 'Failed to create RFQ' });
  }
};

export const getRfqs = async (req, res) => {
  try {
    let query = {};
    if (req.user.role === 'buyer') {
      query = { Buyer: req.user._id };
    } else if (req.user.role === 'vendor') {
      query = { status: 'open' };
    }

    const rfqs = await Rfq.find(query).populate('Buyer', 'fullName companyName email');
    res.status(200).json({ rfqs });
  } catch (error) {
    console.error('Error fetching RFQs:', error);
    res.status(500).json({ message: 'Failed to fetch RFQs' });
  }
};

export const getRfqById = async (req, res) => {
  try {
    const rfq = await Rfq.findById(req.params.id).populate(
      'Buyer',
      'fullName companyName email'
    );

    if (!rfq) {
      return res.status(404).json({ message: 'RFQ not found' });
    }

    if (req.user.role === 'buyer' && rfq.Buyer._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to view this RFQ' });
    }
    if (req.user.role === 'vendor' && rfq.status !== 'open') {
      return res.status(403).json({ message: 'Not authorized to view this RFQ (not open)' });
    }

    res.status(200).json({ rfq });
  } catch (error) {
    console.error('Error fetching RFQ by ID:', error);
    res.status(500).json({ message: 'Failed to fetch RFQ' });
  }
};

export const updateRfq = async (req, res) => {
  const { title, description, budget, deadline, category, status } = req.body;
  const attachments = req.files
    ? req.files.map(file => ({ fileName: file.originalname, filePath: file.path }))
    : [];

  try {
    const rfq = await Rfq.findById(req.params.id);

    if (!rfq) return res.status(404).json({ message: 'RFQ not found' });

    if (rfq.Buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to update this RFQ' });
    }

    rfq.title = title || rfq.title;
    rfq.description = description || rfq.description;
    rfq.budget = budget || rfq.budget;
    rfq.deadline = deadline || rfq.deadline;
    rfq.category = category || rfq.category;
    rfq.status = status || rfq.status;

    if (attachments.length > 0) {
      rfq.attachments = [...rfq.attachments, ...attachments];
    }

    const updatedRfq = await rfq.save();
    res.status(200).json({ message: 'RFQ updated successfully', rfq: updatedRfq });
  } catch (error) {
    console.error('Error updating RFQ:', error);
    res.status(500).json({ message: 'Failed to update RFQ' });
  }
};

export const deleteRfq = async (req, res) => {
  try {
    const rfq = await Rfq.findById(req.params.id);

    if (!rfq) return res.status(404).json({ message: 'RFQ not found' });

    if (rfq.Buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to delete this RFQ' });
    }

    await Rfq.deleteOne({ _id: req.params.id });
    res.status(200).json({ message: 'RFQ deleted successfully' });
  } catch (error) {
    console.error('Error deleting RFQ:', error);
    res.status(500).json({ message: 'Failed to delete RFQ' });
  }
};

export const getQuotationByRFQId = async (req, res) => {
  try {
    const { rfqId } = req.params;

    const rfq = await Rfq.findById(rfqId);
    if (!rfq) return res.status(404).json({ message: 'RFQ not found' });

    const quotations = await Quotation.find({ rfq: rfqId })  // ✅ fixed here
      .populate('vendor', 'fullName companyName email')
      .select('price status deliveryTimeDays attachments createdAt vendorScore')
      .sort({ vendorScore: -1 }); 

    res.status(200).json({ quotations });
  } catch (error) {
    console.error('Error fetching quotations by RFQ:', error);
    res.status(500).json({ message: 'Failed to fetch quotations for RFQ' });
  }
};
