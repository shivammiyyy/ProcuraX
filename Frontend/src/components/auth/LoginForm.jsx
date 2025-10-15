import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from '@/components/ui/spinner';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { LogIn, Eye, EyeOff } from 'lucide-react';

const LoginForm = () => {
  const { login, loading, error } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) return;
    const success = await login(email, password);
    if(success){
      navigate('/dashboard');
    }else{
      console.error(error);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="w-full space-y-5">
      {/* Header */}
      <div className="text-center">
        <div className="flex justify-center mb-2">
          <LogIn className="w-8 h-8 text-blue-600" />
        </div>
        <h2 className="text-2xl font-semibold text-gray-800">Sign In</h2>
        <p className="text-sm text-gray-500">Access your ProcuraX dashboard</p>
      </div>

      {/* Error Message */}
      {error && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-red-50 border border-red-200 text-red-600 text-sm px-3 py-2 rounded-md"
        >
          {error}
        </motion.div>
      )}

      {/* Email */}
      <div>
        <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
          Email Address
        </label>
        <input
          id="email"
          type="email"
          placeholder="Enter your email"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-800 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>

      {/* Password */}
      <div>
        <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
          Password
        </label>
        <div className="relative">
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            placeholder="Enter your password"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 pr-10 text-gray-800 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute inset-y-0 right-3 flex items-center text-gray-400 hover:text-gray-600"
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2.5 rounded-lg transition flex items-center justify-center"
      >
        {loading ? (
          <>
            <Spinner className="text-white mr-2" />
            Logging in...
          </>
        ) : (
          'Login'
        )}
      </button>

      {/* Signup Link */}
      <p className="text-center text-sm text-gray-600">
        Don’t have an account?{' '}
        <button
          type="button"
          onClick={() => navigate('/signup')}
          className="text-blue-600 hover:underline font-medium"
        >
          Sign Up
        </button>
      </p>
    </form>
  );
};

export default LoginForm;
