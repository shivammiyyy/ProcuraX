import React, { useState } from 'react';
import { useAuth } from '../../context/authContext';

const RegisterForm = () => {
  const { registerEmail, loading, error } = useAuth();
  const [email, setEmail] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => {
    setEmail(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSuccess(false);
    try {
      await registerEmail(email); // assuming this returns a promise
      setSuccess(true);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white p-8 rounded shadow-md w-full max-w-md mx-auto"
    >
      <h2 className="text-2xl font-bold mb-6 text-center">Register</h2>

      {error && (
        <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">
          {error}
        </div>
      )}

      {success && (
        <div className="bg-green-100 text-green-700 p-2 mb-4 rounded">
          Registration email sent successfully!
        </div>
      )}

      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="email">
          Email
        </label>
        <input
          id="email"
          name="email"
          type="email"
          className="w-full border rounded px-3 py-2"
          value={email}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded transition"
      >
        {loading ? 'Registering...' : 'Register'}
      </button>
    </form>
  );
};

export default RegisterForm;
