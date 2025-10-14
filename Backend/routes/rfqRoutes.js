import express from 'express';
import { createRfq, deleteRfq, getRfqById, getRfqs, updateRfq } from '../controllers/rfqController.js';
import { uploadRfqAttachments } from '../middleware/upload..js';
import protect from '../middleware/authMiddleware.js';
import { getQuotationByRFQId } from '../controllers/rfqController.js';
const router = express.Router();

const authorizeRoles = (...roles) => (req, res, next) => {
  if (!req.user || !roles.includes(req.user.role)) {
    return res.status(403).json({ message: `Access denied. Requires one of: ${roles.join(', ')} roles.` });
  }
  next();
};

router.route('/')
  .post(protect, authorizeRoles('buyer'), uploadRfqAttachments.array('attachments', 5), createRfq) // 5 max attachments
  .get(protect, getRfqs);

router.route('/:id')
  .get(protect, getRfqById)
  .put(protect, authorizeRoles('buyer'), uploadRfqAttachments.array('attachments', 5), updateRfq)
  .delete(protect, authorizeRoles('buyer'), deleteRfq);

router.get('/:rfqId/quotations', protect, getQuotationByRFQId); // ✅ fixed route

export default router;

