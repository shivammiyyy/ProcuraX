import React, { useState } from 'react';
import { useAuth } from '../../context/authContext';
import { Spinner } from '../ui/spinner';
import { motion } from 'framer-motion';

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
    <motion.form
      onSubmit={handleSubmit}
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="bg-white shadow-xl rounded-2xl p-8 w-full max-w-md border border-gray-100"
    >
      <h2 className="text-3xl font-bold text-center mb-6 text-gray-800">Create Account</h2>

      {error && (
        <div className="bg-red-100 text-red-700 p-2 rounded mb-4 text-sm">{error}</div>
      )}

      {successMsg && (
        <div className="bg-green-100 text-green-700 p-2 rounded mb-4 text-sm">{successMsg}</div>
      )}

      <div className="mb-6">
        <label className="block text-gray-700 mb-1 font-semibold">Email</label>
        <input
          type="email"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 rounded-lg transition flex items-center justify-center"
      >
        {loading ? <Spinner className="text-white mr-2" /> : null}
        {loading ? 'Sending OTP...' : 'Register'}
      </button>
    </motion.form>
  );
};

export default RegisterForm;
