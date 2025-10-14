import { useParams } from 'react-router-dom';
import Navbar from '../../components/common/Navbar';
import QuotationForm from '../../components/quotation/QuotationForm';

export default function QuotationCreatePage() {
  const { rfqId } = useParams();

  return (
    <>
      <Navbar />
      <div className="p-8 flex items-center justify-center">
        <QuotationForm rfqId={rfqId} />
      </div>
    </>
  );
}