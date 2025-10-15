import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from '../ui/spinner';
import { motion } from 'framer-motion';
import { MailPlus } from 'lucide-react';

const RegisterForm = ({ onSuccess }) => {
  const { registerEmail, loading, error } = useAuth();
  const [email, setEmail] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await registerEmail(email);
      setSuccessMsg(res.message);
      onSuccess(email);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="w-full space-y-5">
      <div className="text-center">
        <div className="flex justify-center mb-2">
          <MailPlus className="w-8 h-8 text-indigo-600" />
        </div>
        <h2 className="text-2xl font-semibold text-gray-800">Create Account</h2>
        <p className="text-sm text-gray-500">Start your journey with ProcuraX</p>
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

      {successMsg && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-green-50 border border-green-200 text-green-700 text-sm px-3 py-2 rounded-md"
        >
          {successMsg}
        </motion.div>
      )}

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Email Address
        </label>
        <input
          type="email"
          placeholder="Enter your email"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-800 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 hover:bg-indigo-700 text-white font-medium py-2.5 rounded-lg transition flex items-center justify-center"
      >
        {loading ? (
          <>
            <Spinner className="text-white mr-2" />
            Sending OTP...
          </>
        ) : (
          'Register'
        )}
      </button>
    </form>
  );
};

export default RegisterForm;
