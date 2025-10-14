import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createContract } from '../../api/contractApi';
import { useAuth } from '../../context/AuthContext';

const ContractForm = ({ rfqId, vendorId, buyerId, quotationId }) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [formData, setFormData] = useState({
    rfqId: '',
    vendorId: '',
    buyerId: '',
    quotationId: '',
    content: '',
    startDate: '',
    endDate: '',
    contractFile: null,
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  // Normalize buyerId to string for safe comparison
  const buyerIdStr = buyerId?._id || buyerId || '';
  console.log(buyerIdStr);

  useEffect(() => {
    console.log(buyerIdStr);
    if (!user || user.role !== 'buyer' || String(user._id) !== String(buyerIdStr)) {
      setError('You are not authorized to create this contract.');
      return;
    }

    setFormData((prev) => ({
      ...prev,
      rfqId: rfqId || '',
      vendorId: vendorId || '',
      buyerId: buyerIdStr,
      quotationId: quotationId || '',
    }));
  }, [rfqId, vendorId, buyerIdStr, quotationId, user]);

  const handleChange = (e) => {
    const { name, value, files } = e.target;
    if (name === 'contractFile') {
      setFormData({ ...formData, contractFile: files[0] });
    } else {
      setFormData({ ...formData, [name]: value });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!user || user.role !== 'buyer' || String(user._id) !== String(buyerIdStr)) {
      setError('You are not authorized to create this contract.');
      return;
    }

    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      const data = new FormData();
      Object.entries(formData).forEach(([key, value]) => {
        if (value) data.append(key, value);
      });

      const response = await createContract(data);
      setMessage(response.message || 'Contract created successfully!');
      setTimeout(() => navigate('/dashboard'), 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create contract');
    } finally {
      setLoading(false);
    }
  };

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl mx-auto border border-gray-200"
      encType="multipart/form-data"
    >
      <h2 className="text-3xl font-bold mb-6 text-gray-800 text-center">Create Contract</h2>
      {message && (
        <div className="bg-green-100 text-green-700 border border-green-400 p-2 mb-4 rounded text-center">
          {message}
        </div>
      )}
      {['rfqId', 'vendorId', 'buyerId', 'quotationId'].map((field) => (
        <input key={field} type="hidden" name={field} value={formData[field]} />
      ))}

      <div className="mb-4">
        <label className="block mb-1 font-semibold text-gray-700" htmlFor="content">
          Contract Content
        </label>
        <textarea
          id="content"
          name="content"
          className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          value={formData.content}
          onChange={handleChange}
          disabled={loading}
          rows={4}
          required
          placeholder="Enter key contract terms and clauses..."
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block mb-1 font-semibold text-gray-700" htmlFor="startDate">
            Start Date
          </label>
          <input
            id="startDate"
            name="startDate"
            type="date"
            className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={formData.startDate}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </div>
        <div>
          <label className="block mb-1 font-semibold text-gray-700" htmlFor="endDate">
            End Date
          </label>
          <input
            id="endDate"
            name="endDate"
            type="date"
            className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={formData.endDate}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </div>
      </div>

      <div className="mt-4">
        <label className="block mb-1 font-semibold text-gray-700" htmlFor="contractFile">
          Upload Contract File (optional)
        </label>
        <input
          id="contractFile"
          name="contractFile"
          type="file"
          accept=".pdf,.docx"
          onChange={handleChange}
          disabled={loading}
          className="w-full border border-gray-300 rounded-md px-3 py-2"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className={`mt-6 w-full py-2 rounded-md text-white font-semibold ${
          loading ? 'bg-gray-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'
        }`}
      >
        {loading ? 'Creating Contract...' : 'Create Contract'}
      </button>
    </form>
  );
};

export default ContractForm;
