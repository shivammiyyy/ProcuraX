import Navbar from '../../components/common/Navbar';
import QuotationForm from '../../components/quotation/QuotationForm';

export default function QuotationCreatePage() {
  return (
    <>
      <Navbar />
      <div className="p-8">
        <QuotationForm />
      </div>
    </>
  );
}
