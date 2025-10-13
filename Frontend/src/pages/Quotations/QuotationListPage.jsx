import Navbar from '../../components/common/Navbar';
import React, { useEffect, useState } from 'react';
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
        setQuotations(response.quotations);
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
        {loading && <p>Loading quotations...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && <QuotationList quotations={quotations} />}
      </div>
    </>
  );
}
