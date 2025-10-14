import React from 'react';
import Navbar from '../../components/common/Navbar';
import ContractDetails from '../../components/contract/ContractDetails';

const ContractDetailsPage = () => {
  return (
    <>
      <Navbar />
      <div className="p-8 min-h-screen bg-gray-50">
        <h1 className="text-3xl font-bold mb-6">Contract Details</h1>
        <ContractDetails />
      </div>
    </>
  );
};

export default ContractDetailsPage;