import Button from '@/components/common/Button';
import BeadArt from '@/components/common/BeadArt';

export default function NotFoundPage() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center px-4 py-20 text-center">
      <div className="media-frame size-48 rounded-full">
        <BeadArt
          product={{ id: '404', name: '404', category: 'bracelet', palette: undefined }}
          decorative
          className="h-full w-full"
        />
      </div>

      <h1 className="font-display mt-8 text-[40px] leading-none font-semibold">줄이 끊어졌어요</h1>
      <p className="mt-3 text-[15px] text-ink-soft">
        찾으시는 페이지가 없습니다. 주소가 바뀌었거나 삭제된 것 같아요.
      </p>

      <div className="mt-8 flex gap-2">
        <Button to="/" variant="outline">
          홈으로
        </Button>
        <Button to="/products">상품 보러 가기</Button>
      </div>
    </div>
  );
}
