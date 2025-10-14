import React from 'react';
import Navbar from '../../components/common/Navbar';
import QuotationDetails from '../../components/quotation/QuotationDetails';

export default function QuotationDetailsPage() {
  return (
    <>
      <Navbar />
      <div className="p-8">
        <h1 className="text-3xl font-bold mb-6">Quotation Details</h1>
        <QuotationDetails />
      </div>
    </>
  );
}