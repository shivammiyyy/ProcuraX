import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from '@/components/ui/spinner';
import { motion } from 'framer-motion';
import { ShieldCheck } from 'lucide-react';

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
    <form onSubmit={handleSubmit} className="w-full space-y-4">
      <div className="text-center">
        <div className="flex justify-center mb-2">
          <ShieldCheck className="w-8 h-8 text-green-600" />
        </div>
        <h2 className="text-2xl font-semibold text-gray-800">Verify Your Email</h2>
        <p className="text-sm text-gray-500">
          Enter the 6-digit OTP sent to <strong>{email}</strong>
        </p>
      </div>

      {error && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-red-50 border border-red-200 text-red-600 text-sm px-3 py-2 rounded-md"
        >
          {error}
        </motion.div>
      )}

      <input
        type="text"
        placeholder="Enter OTP"
        maxLength={6}
        value={otp}
        onChange={(e) => setOtp(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-center text-lg tracking-widest focus:ring-2 focus:ring-green-500 outline-none transition"
        required
      />

      <input
        type="password"
        placeholder="Password"
        className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-500 outline-none"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
      />

      <input
        type="text"
        placeholder="Full Name"
        className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-500 outline-none"
        value={fullName}
        onChange={(e) => setFullName(e.target.value)}
        required
      />

      <input
        type="text"
        placeholder="Company Name"
        className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-500 outline-none"
        value={companyName}
        onChange={(e) => setCompanyName(e.target.value)}
        required
      />

      <select
        value={role}
        onChange={(e) => setRole(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-500 outline-none"
      >
        <option value="buyer">Buyer</option>
        <option value="vendor">Vendor</option>
      </select>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-medium py-2.5 rounded-lg transition flex items-center justify-center"
      >
        {loading ? (
          <>
            <Spinner className="text-white mr-2" />
            Verifying...
          </>
        ) : (
          'Complete Signup'
        )}
      </button>
    </form>
  );
};

export default VerifyEmailForm;
