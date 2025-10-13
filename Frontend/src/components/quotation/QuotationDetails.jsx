import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getQuotationById, updateQuotation } from '../../api/quotationApi';
import Navbar from '../../components/common/Navbar';

const QuotationDetails = () => {
  const { id } = useParams();
  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const response = await getQuotationById(id);
        const data = response?.data?.quotation;
        setQuotation(data);

        // ✅ Mark as "in_progress" if not already
        if (data && data.status !== 'in_progress') {
          await updateQuotation(id, { status: 'in_progress' });
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load quotation details');
      } finally {
        setLoading(false);
      }
    };

    fetchQuotation();
  }, [id]);

  return (
    <>
      <Navbar />
      <div className="p-8">
        {loading && <p>Loading quotation details...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && quotation && (
          <div className="bg-white p-8 rounded shadow max-w-3xl mx-auto">
            <h2 className="text-2xl font-bold mb-4">Quotation Details</h2>
            <p><strong>Vendor:</strong> {quotation.vendor?.companyName || quotation.vendor?.fullName}</p>
            <p><strong>Amount:</strong> ₹{quotation.amount}</p>
            <p><strong>Status:</strong> {quotation.status}</p>
            <p><strong>Notes:</strong> {quotation.notes || 'No additional details'}</p>
          </div>
        )}
      </div>
    </>
  );
};

export default QuotationDetails;
