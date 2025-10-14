import React, { useEffect, useState } from 'react';
import Navbar from '../../components/common/Navbar';
import { getQuotations } from '../../api/quotationApi';
import QuotationList from '../../components/quotation/QuotationList';

export default function QuotationListPage() {
  const [quotations, setQuotations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchQuotations = async () => {
      try {
        const response = await getQuotations();
        setQuotations(response.quotations || []);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load quotations');
      } finally {
        setLoading(false);
      }
    };
    fetchQuotations();
  }, []);

  return (
    <>
      <Navbar />
      <div className="p-8">
        <h1 className="text-3xl font-bold mb-6">All Quotations</h1>
        {loading && <p>Loading quotations...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && <QuotationList quotations={quotations} />}
      </div>
    </>
  );
}