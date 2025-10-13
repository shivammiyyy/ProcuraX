import jwt from 'jsonwebtoken';
import User from '../models/userModel.js';
import { sendEmail } from '../utils/mailer.js';

const generateToken = (id) => {
  return jwt.sign({ id }, process.env.JWT_SECRET, { expiresIn: '7d' });
};

export const registerEmail = async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ message: 'Email is required' });

  try {
    const userExists = await User.findOne({ email });
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    if (userExists) {
      if (!userExists.isVerified) {
        userExists.otp = otp;
        await userExists.save();

        await sendEmail(
          email,
          'Verify your email',
          `<p>Your OTP is <b>${otp}</b>. It expires in 1 hour.</p>`
        );

        return res.status(200).json({ message: 'OTP resent successfully' });
      } else {
        return res.status(400).json({ message: 'User already exists and is verified' });
      }
    }

    const user = await User.create({
      email,
      otp,
      isVerified: false,
    });

    await sendEmail(
      email,
      'Verify your email',
      `<p>Your OTP is <b>${otp}</b>. It expires in 1 hour.</p>`
    );
    res.status(201).json({
      message: 'User registered successfully. Please verify your email.',
      userId: user._id,
    });
  } catch (error) {
    console.error('Error registering user:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};

export const verifyEmailAndSignup = async (req, res) => {
  const { email, otp, companyName, password, role, fullName } = req.body;
  if (!email || !otp || !password || !fullName) {
    return res.status(400).json({ message: 'email, otp, fullName and password are required' });
  }

  try {
    const user = await User.findOne({ email });
    if (!user) {
      return res.status(400).json({ message: 'User is not created and OTP is not sent' });
    }

    if (!user.otp || user.otp !== otp) {
      return res.status(400).json({ message: 'Invalid OTP' });
    }

    user.fullName = fullName;
    user.companyName = companyName || '';
    user.password = password;
    user.role = role || 'buyer';
    user.isVerified = true;
    user.otp = '';

    await user.save();

    const token = generateToken(user._id);
    const { password: pw, otp: _otp, ...userSafe } = user.toObject();

    res.status(200).json({
      message: 'Email verified and user signed up successfully',
      token,
      user: userSafe,
    });
  } catch (error) {
    console.error('Error verifying email:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};

export const authUser = async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password)
    return res.status(400).json({ message: 'Email and password required' });

  try {
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ message: 'Invalid email or password' });

    const isMatch = await user.matchPassword(password);
    if (!isMatch) return res.status(400).json({ message: 'Invalid email or password' });

    const token = generateToken(user._id);
    const { password: pw, otp, ...userSafe } = user.toObject();

    res.status(200).json({
      message: 'User authenticated successfully',
      token,
      user: userSafe,
    });
  } catch (error) {
    console.error('Error authenticating user:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};

export const getCurrentUser = async (req, res) => {
  try {
    if (!req.user || !req.user._id) return res.status(401).json({ message: 'Not authorized' });
    const user = await User.findById(req.user._id).select('-password -otp');
    if (!user) return res.status(404).json({ message: 'User not found' });
    res.status(200).json({ user });
  } catch (err) {
    console.error('Error in getCurrentUser:', err);
    res.status(500).json({ message: 'Internal Server Error' });
  }
};
