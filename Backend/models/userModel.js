import mongoose from 'mongoose';
import bcrypt from 'bcryptjs';

const userSchema = new mongoose.Schema(
  {
    fullName: { type: String, default: '' }, 
    email: { type: String, required: true, unique: true },
    password: { type: String, default: '' },
    companyName: { type: String, default: '' },
    role: { type: String, enum: ['buyer', 'admin', 'vendor'], default: 'buyer' },
    isVerified: { type: Boolean, default: false },
    otp: { type: String, default: '' },
  },
  {
    timestamps: true,
  }
);

// only hash when password is set/modified
userSchema.pre('save', async function (next) {
  if (!this.isModified('password')) return next();
  try {
    const salt = await bcrypt.genSalt(10);
    this.password = await bcrypt.hash(this.password, salt);
    next();
  } catch (err) {
    next(err);
  }
});

userSchema.methods.matchPassword = async function (enteredPassword) {
  if (!this.password) return false;
  return await bcrypt.compare(enteredPassword, this.password);
};

const User = mongoose.model('User', userSchema);
export default User;
