import Navbar from '../../components/common/Navbar';
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getQuotationById } from '../../api/quotationApi';
import QuotationDetails from '../../components/quotation/QuotationDetails';

export default function QuotationDetailsPage() {
  const { id } = useParams();
  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const response = await getQuotationById(id);
        setQuotation(response.quotation);
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
        {loading && <p>Loading...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && <QuotationDetails quotation={quotation} />}
      </div>
    </>
  );
}
