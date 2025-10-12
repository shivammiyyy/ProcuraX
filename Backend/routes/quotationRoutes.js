import express from 'express';
import { createQuotation, deleteQuotation, getQuotationById, getQuotations, updateQuotation } from '../controllers/quotationController.js';
import protect from '../middleware/authMiddleware.js';
import { uploadQuotationAttachments } from '../middleware/upload..js';

const router = express.Router();

const authorizeRoles = (...roles) => (req, res, next) => {
  if (!req.user || !roles.includes(req.user.role)) {
    return res.status(403).json({ message: `Access denied. Requires one of: ${roles.join(', ')} roles.` });
  }
  next();
};

router.route('/')
  .post(protect, authorizeRoles('vendor'), uploadQuotationAttachments.array('attachments', 5), createQuotation)
  .get(protect, getQuotations); // Accessible by Admin, Buyer, Vendor (with filters)

router.route('/:id')
  .get(protect, getQuotationById)
  .put(protect, authorizeRoles('vendor', 'buyer'), uploadQuotationAttachments.array('attachments', 5), updateQuotation) // Vendor can update their own, Buyer can update status
  .delete(protect, authorizeRoles('vendor'), deleteQuotation);

export default router;