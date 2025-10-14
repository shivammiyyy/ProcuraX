import React, { useEffect, useState } from 'react';
import Navbar from '../../components/common/Navbar';
import { useParams } from 'react-router-dom';
import { getRfqById } from '../../api/rfqApi';
import RfqDetails from '../../components/rfq/RfqDetails';

export default function RfqDetailsPage() {
  const { id } = useParams();
  const [rfq, setRfq] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRfq = async () => {
      try {
        const response = await getRfqById(id);
        setRfq(response?.data?.rfq || null);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load RFQ details');
      } finally {
        setLoading(false);
      }
    };
    fetchRfq();
  }, [id]);

  return (
    <>
      <Navbar />
      <div className="p-8">
        {loading && <p>Loading RFQ details...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && rfq && <RfqDetails rfq={rfq} />}
      </div>
    </>
  );
}