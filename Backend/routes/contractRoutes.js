import express from 'express';
import  protect  from '../middleware/authMiddleware.js';
import { createContract, getContracts, getContractById, updateContract } from '../controllers/contractController.js';
import { uploadContractDocument } from '../middleware/upload..js';

const router = express.Router();

const authorizeRoles = (...roles) => (req, res, next) => {
  if (!req.user || !roles.includes(req.user.role)) {
    return res.status(403).json({ message: `Access denied. Requires one of: ${roles.join(', ')} roles.` });
  }
  next();
};

router.route('/')
  .post(protect, authorizeRoles('buyer'), uploadContractDocument.single('contractFile'), createContract)
  .get(protect, getContracts); // Accessible by Admin, Buyer, Vendor (with filters)

router.route('/:id')
  .get(protect, getContractById)
  .put(protect, authorizeRoles('buyer', 'admin'), updateContract);

export default router;