import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getContractById, updateContract } from '../../api/contractApi';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/button';

const ContractDetails = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const [contract, setContract] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    const fetchContract = async () => {
      try {
        const response = await getContractById(id);
        setContract(response.data.contract);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load contract details');
      } finally {
        setLoading(false);
      }
    };
    fetchContract();
  }, [id]);

  if (loading) return <p>Loading contract details...</p>;
  if (error) return <p className="text-red-600">{error}</p>;
  if (!contract) return <p>No contract found.</p>;

  // Restrict access to buyer or vendor involved in the contract
  const isBuyer = user.role === 'buyer' && contract.buyer._id === user._id;
  const isVendor = user.role === 'vendor' && contract.vendor._id === user._id;

  if (!isBuyer && !isVendor) {
    return <p className="text-red-600">You are not authorized to view this contract.</p>;
  }

  // Helper function to format dates
  const formatDate = (dateStr) => (dateStr ? new Date(dateStr).toLocaleDateString() : 'N/A');

  // Handle vendor action
  const handleVendorAction = async (newStatus) => {
    setUpdating(true);
    try {
      const updatedContract = await updateContract(contract._id, { status: newStatus });
      setContract(updatedContract.data.contract); // Update local state
    } catch (err) {
      console.error('Failed to update contract:', err);
      alert(err.response?.data?.message || 'Failed to update contract');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="bg-white p-8 rounded shadow-md max-w-4xl mx-auto space-y-6">
      {/* Contract Header */}
      <div className="border-b pb-4 mb-4">
        <h2 className="text-3xl font-bold">{contract.rfq?.title || 'Untitled Contract'}</h2>
        <p className="text-gray-700">{contract.rfq?.description}</p>
      </div>

      {/* Contract Parties */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <h3 className="font-semibold text-lg mb-1">Buyer</h3>
          <p>{contract.buyer?.fullName}</p>
          <p className="text-gray-500">{contract.buyer?.companyName}</p>
        </div>
        <div>
          <h3 className="font-semibold text-lg mb-1">Vendor</h3>
          <p>{contract.vendor?.fullName}</p>
          <p className="text-gray-500">{contract.vendor?.companyName}</p>
        </div>
      </div>

      {/* Contract Dates & Status */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
        <div>
          <p className="font-semibold">Start Date:</p>
          <p>{formatDate(contract.startDate)}</p>
        </div>
        <div>
          <p className="font-semibold">End Date:</p>
          <p>{formatDate(contract.endDate)}</p>
        </div>
        <div>
          <p className="font-semibold">Status:</p>
          <p className="capitalize">{contract.status}</p>
        </div>
      </div>

      {/* Quotation */}
      {contract.quotation && (
        <div>
          <h3 className="font-semibold text-lg mt-4">Quotation</h3>
          <p>Price: ₹{contract.quotation.price.toLocaleString()}</p>
        </div>
      )}

      {/* Contract File */}
      {contract.contractFile?.filePath && (
        <div className="mt-4">
          <h3 className="font-semibold text-lg">Contract Document</h3>
          <a
            href={contract.contractFile.filePath}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline"
            download={contract.contractFile.fileName}
          >
            Download {contract.contractFile.fileName}
          </a>
        </div>
      )}

      {/* Contract Content */}
      {contract.content && (
        <div className="mt-4">
          <h3 className="font-semibold text-lg">Contract Content</h3>
          <pre className="whitespace-pre-wrap bg-gray-50 p-4 rounded border border-gray-200">{contract.content}</pre>
        </div>
      )}

      {/* Audit Report */}
      {contract.auditReport && (
        <div className="mt-4 p-4 bg-yellow-100 border-l-4 border-yellow-400 text-yellow-700 rounded">
          <h3 className="font-semibold mb-2">AI Contract Audit Report</h3>
          <pre className="whitespace-pre-wrap">{contract.auditReport}</pre>
        </div>
      )}

      {/* Audit Warnings */}
      {contract.auditWarnings?.length > 0 && (
        <div className="mt-4 p-4 bg-red-50 border-l-4 border-red-400 text-red-700 rounded">
          <h3 className="font-semibold mb-2">Audit Warnings</h3>
          <ul className="list-disc list-inside space-y-1">
            {contract.auditWarnings.map((warning) => (
              <li key={warning._id}>
                <span className="font-semibold">{warning.severity}:</span> {warning.description}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Vendor Actions */}
      {isVendor && contract.status === 'Active' && (
        <div className="mt-6 flex gap-4">
          <Button
            className="bg-green-600 hover:bg-green-700"
            disabled={updating}
            onClick={() => handleVendorAction('Audited')}
          >
            Accept
          </Button>
          <Button
            className="bg-red-600 hover:bg-red-700"
            disabled={updating}
            onClick={() => handleVendorAction('cancelled')}
          >
            Cancel
          </Button>
        </div>
      )}
    </div>
  );
};

export default ContractDetails;
