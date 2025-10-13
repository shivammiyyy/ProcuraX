import React, { useState } from 'react';
import { useAuth } from '../../context/authContext';

const VerifyEmailForm = ({ email }) => {
  const { verifyEmailAndSignup, loading, error } = useAuth();

  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [role, setRole] = useState('buyer'); // default role

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await verifyEmailAndSignup(email, otp, password, companyName, role, fullName);
      // You can show success message or redirect user here
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white p-8 rounded shadow-md w-full max-w-md mx-auto"
    >
      <h2 className="text-2xl font-bold mb-6 text-center">Verify Email</h2>
      <p className="mb-6 text-center">
        Enter the OTP sent to your email: <strong>{email}</strong>
      </p>

      {error && (
        <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{error}</div>
      )}

      {/* OTP Input */}
      <input
        type="text"
        maxLength={6}
        className="w-full border rounded px-3 py-2 mb-6 text-center text-xl tracking-widest"
        placeholder="Enter OTP"
        value={otp}
        onChange={(e) => setOtp(e.target.value)}
        disabled={loading}
        required
      />

      {/* Password */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="password">
          Password
        </label>
        <input
          id="password"
          type="password"
          className="w-full border rounded px-3 py-2"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={loading}
          required
        />
      </div>

      {/* Full Name */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="fullName">
          Full Name
        </label>
        <input
          id="fullName"
          type="text"
          className="w-full border rounded px-3 py-2"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          disabled={loading}
          required
        />
      </div>

      {/* Company Name */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="companyName">
          Company Name
        </label>
        <input
          id="companyName"
          type="text"
          className="w-full border rounded px-3 py-2"
          value={companyName}
          onChange={(e) => setCompanyName(e.target.value)}
          disabled={loading}
          required
        />
      </div>

      {/* Role Dropdown */}
      <div className="mb-6">
        <label className="block mb-1 font-semibold" htmlFor="role">
          Role
        </label>
        <select
          id="role"
          value={role}
          onChange={(e) => setRole(e.target.value)}
          className="w-full border rounded px-3 py-2"
          disabled={loading}
          required
        >
          <option value="buyer">Buyer</option>
          <option value="vendor">Vendor</option>
        </select>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 rounded transition"
      >
        {loading ? 'Verifying...' : 'Verify'}
      </button>
    </form>
  );
};

export default VerifyEmailForm;
