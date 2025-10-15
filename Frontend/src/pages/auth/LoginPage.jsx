import React from 'react';
import { motion } from 'framer-motion';
import LoginForm from '../../components/auth/LoginForm';

const LoginPage = () => {
  return (
    <div
      className="relative min-h-screen flex items-center justify-center bg-cover bg-center"
      style={{
        backgroundImage: `url('https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1400&q=80')`,
      }}
    >
      {/* Overlay */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />

      {/* Main content */}
      <motion.div
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="relative z-10 flex flex-col md:flex-row items-center justify-center gap-12 px-6"
      >
        {/* Left side (text) */}
        <div className="hidden md:flex flex-col text-white max-w-md space-y-4">
          <h1 className="text-5xl font-extrabold leading-tight drop-shadow-lg">
            Welcome Back to <span className="text-blue-400">ProcuraX</span>
          </h1>
          <p className="text-lg text-gray-200">
            Manage RFQs, quotations, and streamline your procurement process with ease.
          </p>
        </div>

        {/* Right side (Login form) */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="bg-white/90 backdrop-blur-lg rounded-2xl shadow-2xl p-6 w-full max-w-md border border-white/30"
        >
          <LoginForm />
        </motion.div>
      </motion.div>
    </div>
  );
};

export default LoginPage;
