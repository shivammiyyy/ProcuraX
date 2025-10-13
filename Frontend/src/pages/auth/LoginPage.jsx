import React from 'react';
import LoginForm from '../../components/auth/LoginForm';

const LoginPage = () => {
  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-gray-50">
      {/* Left Section */}
      <div className="hidden md:flex flex-1 bg-blue-600 text-white flex-col justify-center items-center p-12">
        <h1 className="text-4xl font-bold mb-4">Welcome Back!</h1>
        <p className="text-lg text-blue-100 text-center max-w-md">
          Manage your procurement workflow and collaborate with vendors seamlessly.
        </p>
        <img
          src="/assets/login-illustration.svg"
          alt="ProcuraX Login"
          className="w-3/4 mt-10"
        />
      </div>

      {/* Right Section */}
      <div className="flex-1 flex items-center justify-center px-6 py-12">
        <LoginForm />
      </div>
    </div>
  );
};

export default LoginPage;
