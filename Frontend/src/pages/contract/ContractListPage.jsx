import React from 'react';
import Navbar from '../../components/common/Navbar';
import ContractList from '../../components/contract/ContractList';

const ContractListPage = () => {
  return (
    <>
      <Navbar />
      <div className="p-8 min-h-screen bg-gray-50">
        <h1 className="text-3xl font-bold mb-6">All Contracts</h1>
        <ContractList />
      </div>
    </>
  );
};

export default ContractListPage;