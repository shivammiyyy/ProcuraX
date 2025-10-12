import Contract from "../models/contractModel.js";
import { auditContractWithGemini } from "../utils/geminiService.js";

// @desc    Create a new contract (after a quotation is accepted)
// @route   POST /api/v0/contract
// @access  Private/Buyer
export const createContract = async (req, res) => {
    const { rfqId, vendorId, buyerId, quotationId, content, startDate, endDate } = req.body;
    const contractFile = req.files ? req.files.map(file => ({
      fileName: file.originalname,
      filePath: file.path, // Cloudinary URL
    })) : [];
    
    if(!rfqId || !vendorId || !buyerId || !quotationId || !content || !startDate || !endDate || contractFile.length === 0) {
        return res.status(400).json({ message: 'Please enter all required fields for the contract.' });
      }
    const geminiAudit = await auditContractWithGemini(content);

    const newContract = new Contract({
        rfq: rfqId,
        vendor: vendorId,
        buyer: buyerId,
        quotation: quotationId,
        content,
        contractFile: contractFile[0], // Assuming single file upload
        startDate,
        endDate,
        auditStatus: geminiAudit.auditStatus === 'Failed' ? 'Failed' : 'Completed',
        auditReport: geminiAudit.auditReport,
        auditWarnings: geminiAudit.auditWarnings,
    });

    try {
        await newContract.save();
        res.status(201).json({ message: 'Contract created successfully.', contract: newContract });
    } catch (error) {
        console.error('Error creating contract:', error);
        res.status(500).json({ message: 'Internal Server Error' });
    }
}

// @desc    Get all contracts (filtered by role)
// @route   GET /api/v0/contract
// @access  Private/Admin/Buyer/Vendor
export const getContracts = async (req, res) => {
  try {
    let query = {};
    if (req.user.role === 'buyer') {
      query = { buyer: req.user._id };
    } else if (req.user.role === 'vendor') {
      query = { vendor: req.user._id };
    }

    const contracts = await Contract.find(query)
      .populate('rfq', 'title description')
      .populate('vendor', 'fullName companyName')
      .populate('buyer', 'fullName companyName')
      .populate('quotation', 'price');

    res.status(200).json({ contracts });
  } catch (error) {
    console.error('Error fetching contracts:', error);
    res.status(500).json({ message: 'Failed to fetch contracts', error: error.message });
  }
};

// @desc    Get a single contract by ID
// @route   GET /api/v0/contract/:id
// @access  Private/Admin/Buyer/Vendor
export const getContractById = async (req, res) => {
  try {
    const contract = await Contract.findById(req.params.id)
      .populate('rfq', 'title description')
      .populate('vendor', 'fullName companyName')
      .populate('buyer', 'fullName companyName')
      .populate('quotation', 'price');

    if (!contract) {
      return res.status(404).json({ message: 'Contract not found' });
    }

    // Authorization: User must be the buyer, vendor, or admin
    if (
      req.user.role === 'buyer' && contract.buyer.toString() !== req.user._id.toString() ||
      req.user.role === 'vendor' && contract.vendor.toString() !== req.user._id.toString()
    ) {
      return res.status(403).json({ message: 'Not authorized to view this contract' });
    }

    res.status(200).json({ contract });
  } catch (error) {
    console.error('Error fetching contract by ID:', error);
    res.status(500).json({ message: 'Failed to fetch contract', error: error.message });
  }
};

// @desc    Update a contract (e.g., status, dates)
// @route   PUT /api/v0/contract/:id
// @access  Private/Buyer (or Admin)
export const updateContract = async (req, res) => {
  const { startDate, endDate, status } = req.body;

  try {
    const contract = await Contract.findById(req.params.id);

    if (!contract) {
      return res.status(404).json({ message: 'Contract not found' });
    }

    // Only buyer who created it or admin can update
    if (req.user.role === 'buyer' && contract.buyer.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized to update this contract.' });
    }
    if (req.user.role !== 'buyer' && req.user.role !== 'admin') {
        return res.status(403).json({ message: 'Only buyers and administrators can update contracts.' });
    }


    contract.startDate = startDate || contract.startDate;
    contract.endDate = endDate || contract.endDate;
    contract.status = status || contract.status;

    const updatedContract = await contract.save();
    res.status(200).json({ message: 'Contract updated successfully', contract: updatedContract });
  } catch (error) {
    console.error('Error updating contract:', error);
    res.status(500).json({ message: 'Failed to update contract', error: error.message });
  }
};

