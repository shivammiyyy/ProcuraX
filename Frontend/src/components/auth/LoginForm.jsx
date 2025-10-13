import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from '@/components/ui/spinner';
import { motion } from 'framer-motion';

const LoginForm = () => {
  const { login, loading, error } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    await login(email, password);
  };

  return (
    <motion.form
      onSubmit={handleSubmit}
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="bg-white shadow-xl rounded-2xl p-8 w-full max-w-md border border-gray-100"
    >
      <h2 className="text-3xl font-bold text-center mb-6 text-gray-800">Sign In</h2>

      {error && (
        <div className="bg-red-100 text-red-700 p-2 mb-4 rounded text-sm">{error}</div>
      )}

      <input
        type="email"
        placeholder="Email"
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />

      <input
        type="password"
        placeholder="Password"
        className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-6"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
      />

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition flex items-center justify-center"
      >
        {loading ? <Spinner className="text-white mr-2" /> : null}
        {loading ? 'Logging in...' : 'Login'}
      </button>
    </motion.form>
  );
};

export default LoginForm;
