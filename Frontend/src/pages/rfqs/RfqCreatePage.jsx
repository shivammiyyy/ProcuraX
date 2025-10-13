import Navbar from '../../components/common/Navbar';
import RfqForm from '../../components/rfq/RfqForm';

export default function RfqCreatePage() {
  return (
    <>
      <Navbar />
      <div className="p-8">
        <RfqForm />
      </div>
    </>
  );
}
