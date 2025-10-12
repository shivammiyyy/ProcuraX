import User from '../models/userModel.js';
import Rfq from '../models/RfqModel.js';
import { sendEmail } from '../utils/mailer.js'; 
import { uploadRfqAttachments } from '../middleware/upload..js';


export const createRfq = async (req, res) => {
    const { title, description, requestType, budget, deadline, category } = req.body;
    const attachment = req.files?.length
  ? req.files.map(file => ({
      fileName: file.originalname,
      filePath: file.path, // Cloudinary uploaded file URL
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
      Buyer: req.user._id, // Assuming authentication middleware sets req.user
      attachment,
    });
    await newRfq.save();

    const vendors = await User.find({ role: 'vendor' }).select('email');
    if (vendors.length > 0) {
      const vendorEmails = vendors.map(vendor => vendor.email);
      await sendEmail(
        vendorEmails.join(','),
        `New ${requestType} Posted: ${title}`,
        `<p>A new ${requestType} titled "<b>${title}</b>" has been posted on the platform. Review the details and submit your quotation!</p>
         <p>Deadline: ${new Date(deadline).toLocaleDateString()}</p>
         <p>Description: ${description}</p>`
      );
    }
    res.status(201).json({ message: 'RFQ created successfully', rfq: newRfq });
  } catch (error) {
    console.error('Error creating RFQ:', error);
    res.status(500).json({ message: 'Failed to create RFQ', error: error.message });
  }
};

export const getRfqs = async (req, res) => {
  try {
    let query = {};
    if (req.user.role === 'buyer') {
      query = { Buyer: req.user._id }; // Buyers only see their own RFQs
    } else if (req.user.role === 'vendor') {
      query = { status: 'open' }; // Vendors only see open RFQs
    }
    // Admin can see all, so no specific query for admin role

    const rfqs = await Rfq.find(query).populate('Buyer', 'fullName companyName email');
    res.status(200).json({ rfqs });
  } catch (error) {
    console.error('Error fetching RFQs:', error);
    res.status(500).json({ message: 'Failed to fetch RFQs', error: error.message });
  }
};


export const getRfqById = async (req, res) => {
  try {
    const rfq = await Rfq.findById(req.params.id).populate('Buyer', 'fullName companyName email');

    if (!rfq) {
      return res.status(404).json({ message: 'RFQ not found' });
    }

    // Authorization: Buyers can only see their own RFQs, Vendors can only see open RFQs
    if (req.user.role === 'buyer' && rfq.Buyer._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to view this RFQ' });
    }
    if (req.user.role === 'vendor' && rfq.status !== 'open') {
      return res.status(403).json({ message: 'Not authorized to view this RFQ (not open)' });
    }

    res.status(200).json({ rfq });
  } catch (error) {
    console.error('Error fetching RFQ by ID:', error);
    res.status(500).json({ message: 'Failed to fetch RFQ', error: error.message });
  }
};

// @desc    Update an RFQ (only by the buyer who created it)
// @route   PUT /api/v0/rfq/:id
// @access  Private/Buyer
export const updateRfq = async (req, res) => {
  const { title, description, budget, deadline, category, status } = req.body;
  const attachments = req.files ? req.files.map(file => ({
    fileName: file.originalname,
    filePath: file.path,
  })) : [];

  try {
    const rfq = await Rfq.findById(req.params.id);

    if (!rfq) {
      return res.status(404).json({ message: 'RFQ not found' });
    }

    // Check if the authenticated user is the buyer who created the RFQ
    if (rfq.Buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to update this RFQ' });
    }

    rfq.title = title || rfq.title;
    rfq.description = description || rfq.description;
    rfq.budget = budget || rfq.budget;
    rfq.deadline = deadline || rfq.deadline;
    rfq.category = category || rfq.category;
    rfq.status = status || rfq.status; // Can update status (e.g., to 'closed')

    // Append new attachments
    if (attachments.length > 0) {
      rfq.attachments = [...rfq.attachments, ...attachments];
    }
    
    const updatedRfq = await rfq.save();
    res.status(200).json({ message: 'RFQ updated successfully', rfq: updatedRfq });
  } catch (error) {
    console.error('Error updating RFQ:', error);
    res.status(500).json({ message: 'Failed to update RFQ', error: error.message });
  }
};

// @desc    Delete an RFQ (only by the buyer who created it)
// @route   DELETE /api/v0/rfq/:id
// @access  Private/Buyer
export const deleteRfq = async (req, res) => {
  try {
    const rfq = await Rfq.findById(req.params.id);

    if (!rfq) {
      return res.status(404).json({ message: 'RFQ not found' });
    }

    // Check if the authenticated user is the buyer who created the RFQ
    if (rfq.Buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to delete this RFQ' });
    }

    await Rfq.deleteOne({ _id: req.params.id }); // Use deleteOne or findByIdAndDelete
    res.status(200).json({ message: 'RFQ deleted successfully' });
  } catch (error) {
    console.error('Error deleting RFQ:', error);
    res.status(500).json({ message: 'Failed to delete RFQ', error: error.message });
  }
};