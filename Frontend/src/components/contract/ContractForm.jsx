import React, { useState } from 'react';
import { createContract } from '../../api/contractApi';

const ContractForm = () => {
  const [formData, setFormData] = useState({
    rfqId: '',
    vendorId: '',
    buyerId: '',
    quotationId: '',
    content: '',
    startDate: '',
    endDate: '',
    file: null,
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value, files } = e.target;
    if (name === 'file') {
      setFormData({ ...formData, file: files[0] });
    } else {
      setFormData({ ...formData, [name]: value });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
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
      setFormData({
        rfqId: '',
        vendorId: '',
        buyerId: '',
        quotationId: '',
        content: '',
        startDate: '',
        endDate: '',
        file: null,
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create contract');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white p-8 rounded shadow-md w-full max-w-lg mx-auto"
      encType="multipart/form-data"
    >
      <h2 className="text-2xl font-bold mb-6">Create Contract</h2>

      {message && <div className="bg-green-100 text-green-700 p-2 mb-4 rounded">{message}</div>}
      {error && <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{error}</div>}

      {['rfqId', 'vendorId', 'buyerId', 'quotationId'].map((field) => (
        <div className="mb-4" key={field}>
          <label className="block mb-1 font-semibold capitalize" htmlFor={field}>
            {field.replace('Id', ' ID')}
          </label>
          <input
            id={field}
            name={field}
            type="text"
            className="w-full border rounded px-3 py-2"
            value={formData[field]}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </div>
      ))}

      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="content">
          Contract Content
        </label>
        <textarea
          id="content"
          name="content"
          className="w-full border rounded px-3 py-2"
          value={formData.content}
          onChange={handleChange}
          disabled={loading}
          rows={4}
          required
        />
      </div>

      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="startDate">
          Start Date
        </label>
        <input
          id="startDate"
          name="startDate"
          type="date"
          className="w-full border rounded px-3 py-2"
          value={formData.startDate}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="endDate">
          End Date
        </label>
        <input
          id="endDate"
          name="endDate"
          type="date"
          className="w-full border rounded px-3 py-2"
          value={formData.endDate}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      <div className="mb-6">
        <label className="block mb-1 font-semibold" htmlFor="file">
          Upload Contract File
        </label>
        <input
          id="file"
          name="file"
          type="file"
          accept=".pdf,.doc,.docx"
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded"
      >
        {loading ? 'Submitting...' : 'Submit Contract'}
      </button>
    </form>
  );
};

export default ContractForm;
