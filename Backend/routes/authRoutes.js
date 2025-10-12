import express from 'express';
import { authUser, getCurrentUser, registerEmail, verifyEmailAndSignup } from '../controllers/authController.js';
import protect from '../middleware/authMiddleware.js';

const router = express.Router();

router.post('/register', registerEmail);
router.post('/verify', verifyEmailAndSignup);
router.post('/login', authUser);
router.get('/getuser', protect, getCurrentUser);

export default router;
