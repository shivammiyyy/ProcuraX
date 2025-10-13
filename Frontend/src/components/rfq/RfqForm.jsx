import React, { useState } from 'react';
import axios from 'axios';

const RfqForm = () => {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    requestType: 'product', // default value
    budget: '',
    deadline: '',
    category: '',
  });

  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // handle input fields
  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  // handle file upload
  const handleFileChange = (e) => {
    setAttachments([...e.target.files]);
  };

  // submit form
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('token'); // assuming auth token stored here
      const data = new FormData();

      Object.entries(formData).forEach(([key, value]) => {
        data.append(key, value);
      });
      attachments.forEach((file) => data.append('files', file));

      const response = await axios.post('/api/v0/rfq', data, {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${token}`,
        },
      });

      setSuccess(response.data.message || 'RFQ created successfully!');
      setFormData({
        title: '',
        description: '',
        requestType: 'product',
        budget: '',
        deadline: '',
        category: '',
      });
      setAttachments([]);
    } catch (err) {
      setError(
        err.response?.data?.message || 'Failed to create RFQ. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white p-8 rounded shadow-md w-full max-w-lg mx-auto"
    >
      <h2 className="text-2xl font-bold mb-6">Create RFQ</h2>

      {error && (
        <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{error}</div>
      )}
      {success && (
        <div className="bg-green-100 text-green-700 p-2 mb-4 rounded">
          {success}
        </div>
      )}

      {/* Title */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="title">
          Title
        </label>
        <input
          id="title"
          name="title"
          type="text"
          className="w-full border rounded px-3 py-2"
          value={formData.title}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      {/* Description */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="description">
          Description
        </label>
        <textarea
          id="description"
          name="description"
          className="w-full border rounded px-3 py-2"
          rows={4}
          value={formData.description}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      {/* Request Type */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="requestType">
          Request Type
        </label>
        <select
          id="requestType"
          name="requestType"
          className="w-full border rounded px-3 py-2"
          value={formData.requestType}
          onChange={handleChange}
          disabled={loading}
          required
        >
          <option value="product">Product</option>
          <option value="service">Service</option>
        </select>
      </div>

      {/* Budget */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="budget">
          Budget (in ₹)
        </label>
        <input
          id="budget"
          name="budget"
          type="number"
          className="w-full border rounded px-3 py-2"
          value={formData.budget}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      {/* Category */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="category">
          Category
        </label>
        <input
          id="category"
          name="category"
          type="text"
          className="w-full border rounded px-3 py-2"
          value={formData.category}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      {/* Deadline */}
      <div className="mb-4">
        <label className="block mb-1 font-semibold" htmlFor="deadline">
          Deadline
        </label>
        <input
          id="deadline"
          name="deadline"
          type="date"
          className="w-full border rounded px-3 py-2"
          value={formData.deadline}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>

      {/* Attachments */}
      <div className="mb-6">
        <label className="block mb-1 font-semibold" htmlFor="attachments">
          Attachments (optional)
        </label>
        <input
          id="attachments"
          name="attachments"
          type="file"
          multiple
          onChange={handleFileChange}
          disabled={loading}
          className="w-full border rounded px-3 py-2"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded"
      >
        {loading ? 'Submitting...' : 'Submit RFQ'}
      </button>
    </form>
  );
};

export default RfqForm;
