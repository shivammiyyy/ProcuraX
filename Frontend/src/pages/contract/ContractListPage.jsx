import React from 'react';
import Navbar from '../../components/common/Navbar';
import ContractList from '../../components/contract/ContractList';

const ContractListPage = () => {
  return (
    <>
      <Navbar />
      <div className="min-h-screen bg-gray-50 py-10 px-6">
        <div className="max-w-6xl mx-auto">
          <h1 className="text-4xl font-bold text-gray-800 mb-8">
            Contracts Overview
          </h1>
          <ContractList />
        </div>
      </div>
    </>
  );
};

export default ContractListPage;
