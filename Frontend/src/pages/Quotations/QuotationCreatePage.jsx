import { useParams } from 'react-router-dom';
import Navbar from '../../components/common/Navbar';
import QuotationForm from '../../components/quotation/QuotationForm';

export default function QuotationCreatePage() {
  const { rfqId } = useParams(); // ✅ extract rfqId from URL

  return (
    <>
      <Navbar />
      <div className="p-8 flex align-items justify-center">
        <QuotationForm  rfqId={rfqId}/>
      </div>
    </>
  );
}
