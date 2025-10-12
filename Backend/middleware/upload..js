import multer from 'multer';
import {CloudinaryStorage} from 'multer-storage-cloudinary';
import cloudinary from '../utils/cloudinary.js';

const rfqStorage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'procurement/rfq_attachments', // Specific folder for RFQ attachments
    allowed_formats: ["jpg", "png", "pdf", "docx"],
  },
});

const quotationStorage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'procurement/quotation_attachments', // Specific folder for quotation attachments
    allowed_formats: ["jpg", "png", "pdf", "docx"],
  },
});

const contractStorage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'procurement/contract_documents', // Specific folder for contract documents
    allowed_formats: ["pdf", "docx"], // Contracts usually only need these formats
  },
});

export const uploadRfqAttachments = multer({ storage: rfqStorage });
export const uploadQuotationAttachments = multer({ storage: quotationStorage });
export const uploadContractDocument = multer({ storage: contractStorage });
