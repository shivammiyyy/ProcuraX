import React, { useState } from 'react';
import { useAuth } from '../../context/authContext';
import { Spinner } from '@/components/ui/spinner';
import { motion } from 'framer-motion';

const VerifyEmailForm = ({ email }) => {
  const { verifyAndSignup, loading, error } = useAuth();
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [role, setRole] = useState('buyer');

  const handleSubmit = async (e) => {
    e.preventDefault();
    await verifyAndSignup({ email, otp, password, companyName, role, fullName });
  };

  return (
    <motion.form
      onSubmit={handleSubmit}
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="bg-white shadow-xl rounded-2xl p-8 w-full max-w-md border border-gray-100"
    >
      <h2 className="text-3xl font-bold text-center mb-6 text-gray-800">Verify Your Email</h2>

      <p className="text-sm text-gray-600 text-center mb-6">
        Enter the OTP sent to <strong>{email}</strong>
      </p>

      {error && <div className="bg-red-100 text-red-700 p-2 mb-4 rounded text-sm">{error}</div>}

      <input
        type="text"
        placeholder="Enter 6-digit OTP"
        maxLength={6}
        value={otp}
        onChange={(e) => setOtp(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4 text-center text-lg tracking-widest"
        required
      />

      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4"
        required
      />

      <input
        type="text"
        placeholder="Full Name"
        value={fullName}
        onChange={(e) => setFullName(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4"
        required
      />

      <input
        type="text"
        placeholder="Company Name"
        value={companyName}
        onChange={(e) => setCompanyName(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4"
        required
      />

      <select
        value={role}
        onChange={(e) => setRole(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-6"
      >
        <option value="buyer">Buyer</option>
        <option value="vendor">Vendor</option>
      </select>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded-lg transition flex items-center justify-center"
      >
        {loading ? <Spinner className="text-white mr-2" /> : null}
        {loading ? 'Verifying...' : 'Complete Signup'}
      </button>
    </motion.form>
  );
};

export default VerifyEmailForm;
