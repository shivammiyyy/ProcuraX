import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getQuotationById, updateQuotation } from '../../api/quotationApi';
import Navbar from '../../components/common/Navbar';

const QuotationDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);
  const [message, setMessage] = useState('');

  // ✅ Fetch quotation details
  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const response = await getQuotationById(id);
        setQuotation(response.data.quotation);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load quotation details.');
      } finally {
        setLoading(false);
      }
    };

    fetchQuotation();
  }, [id]);

  // ✅ Handle Accept / Reject actions
  const handleUpdateStatus = async (newStatus) => {
    setUpdating(true);
    setMessage('');
    try {
      await updateQuotation(id, { status: newStatus });
      setMessage(`Quotation ${newStatus} successfully.`);

      if (newStatus === 'accepted') {
        // Redirect to contract creation page
        setTimeout(() => navigate(`/contracts/create/${id}`), 1000);
      } else {
        // Redirect to dashboard if rejected
        setTimeout(() => navigate('/dashboard'), 1000);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to ${newStatus} quotation.`);
    } finally {
      setUpdating(false);
    }
  };

  return (
    <>
      <Navbar />
      <div className="p-8">
        {loading && <p>Loading quotation details...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {message && <p className="text-green-600">{message}</p>}

        {!loading && !error && quotation && (
          <div className="bg-white p-8 rounded shadow max-w-3xl mx-auto">
            <h2 className="text-2xl font-bold mb-4">Quotation Details</h2>

            <p><strong>Vendor:</strong> {quotation.vendor?.companyName || quotation.vendor?.fullName}</p>
            <p><strong>Price:</strong> ₹{quotation.price}</p>
            <p><strong>Delivery Time:</strong> {quotation.deliveryTimeDays} days</p>
            <p><strong>Status:</strong> {quotation.status}</p>
            <p><strong>Compliance Score:</strong> {quotation.vendorScore?.toFixed(2) || 'N/A'}</p>

            <div className="mt-6 flex gap-4">
              <button
                onClick={() => handleUpdateStatus('accepted')}
                disabled={updating}
                className="bg-green-600 hover:bg-green-700 text-white font-semibold py-2 px-4 rounded"
              >
                {updating ? 'Updating...' : 'Accept & Create Contract'}
              </button>

              <button
                onClick={() => handleUpdateStatus('rejected')}
                disabled={updating}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold py-2 px-4 rounded"
              >
                {updating ? 'Updating...' : 'Reject'}
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default QuotationDetails;
