import multer from 'multer';
import { CloudinaryStorage } from 'multer-storage-cloudinary';
import cloudinary from '../utils/cloudinary.js';

// Cloudinary storage for RFQs
const rfqStorage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'procurement/rfq_attachments', // Folder for RFQ attachments
    allowed_formats: ['jpg', 'png', 'pdf', 'docx'],
  },
});

// Cloudinary storage for quotations
const quotationStorage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'procurement/quotation_attachments',
    allowed_formats: ['jpg', 'png', 'pdf', 'docx'],
  },
});

// Cloudinary storage for contracts
const contractStorage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'procurement/contract_documents',
    allowed_formats: ['pdf', 'docx'],
  },
});

export const uploadRfqAttachments = multer({ storage: rfqStorage });
export const uploadQuotationAttachments = multer({ storage: quotationStorage });
export const uploadContractDocument = multer({ storage: contractStorage });
