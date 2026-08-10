import { useEffect, useRef, useState } from 'react';
import { ImagePlus, Star, X } from 'lucide-react';
import Sheet from '@/components/common/Sheet';
import Button from '@/components/common/Button';
import Field from '@/components/common/Field';
import { useToast } from '@/components/common/Toast';
import { useCreateReviewMutation } from './reviewApi';

const MAX_FILES = 5;

/**
 * 리뷰 작성.
 *  · 별점은 라디오 그룹으로 구현해 키보드로도 선택할 수 있습니다.
 *  · 이미지는 업로드 진행률을 개별로 표시합니다 (여러 장을 한 번에 올릴 때
 *    전체 스피너 하나만 돌면 어디까지 됐는지 알 수 없습니다).
 */
export default function ReviewForm({ productId, open, onClose }) {
  const { toast } = useToast();
  const [createReview, { isLoading }] = useCreateReviewMutation();
  const fileInputRef = useRef(null);
  const timersRef = useRef([]);

  const [rating, setRating] = useState(5);
  const [content, setContent] = useState('');
  const [touched, setTouched] = useState(false);
  const [uploads, setUploads] = useState([]);

  useEffect(() => () => timersRef.current.forEach(clearInterval), []);

  useEffect(() => {
    if (!open) {
      setRating(5);
      setContent('');
      setTouched(false);
      setUploads([]);
    }
  }, [open]);

  const error = touched && content.trim().length < 10 ? '10자 이상 남겨주세요.' : null;

  /** 실제 서비스에서는 XHR의 upload.onprogress 값을 그대로 씁니다. */
  const handleFiles = (event) => {
    const files = [...event.target.files].slice(0, MAX_FILES - uploads.length);
    event.target.value = '';

    files.forEach((file) => {
      const id = `${file.name}-${file.size}-${uploads.length}-${Math.random()}`;
      setUploads((list) => [...list, { id, name: file.name, progress: 0 }]);

      const timer = setInterval(() => {
        setUploads((list) =>
          list.map((u) => {
            if (u.id !== id) return u;
            const next = Math.min(100, u.progress + 12 + Math.random() * 18);
            if (next >= 100) clearInterval(timer);
            return { ...u, progress: next };
          }),
        );
      }, 220);
      timersRef.current.push(timer);
    });
  };

  const uploading = uploads.some((u) => u.progress < 100);

  const submit = async (e) => {
    e.preventDefault();
    setTouched(true);
    if (content.trim().length < 10) return;

    try {
      await createReview({
        productId,
        rating,
        content: content.trim(),
        photoCount: uploads.length,
      }).unwrap();
      toast('리뷰가 등록되었어요. 감사합니다!');
      onClose();
    } catch {
      toast('리뷰 등록에 실패했어요. 잠시 후 다시 시도해 주세요.', { tone: 'error' });
    }
  };

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="리뷰 쓰기"
      description="어떤 점이 좋았는지 알려주세요"
      side="bottom"
      footer={
        <Button
          type="submit"
          form="review-form"
          full
          loading={isLoading}
          disabled={uploading}
        >
          {uploading ? '사진 업로드 중…' : '등록하기'}
        </Button>
      }
    >
      <form id="review-form" onSubmit={submit} className="space-y-6 px-5 py-6">
        <fieldset>
          <legend className="mb-2 text-[13px] font-medium">별점</legend>
          <div className="flex items-center gap-1" role="radiogroup" aria-label="별점 선택">
            {[1, 2, 3, 4, 5].map((score) => (
              <button
                key={score}
                type="button"
                role="radio"
                aria-checked={rating === score}
                aria-label={`${score}점`}
                onClick={() => setRating(score)}
                className="flex size-11 items-center justify-center rounded-full transition-transform active:scale-90"
              >
                <Star
                  size={26}
                  className={score <= rating ? 'text-accent' : 'text-line-strong'}
                  fill="currentColor"
                  strokeWidth={0}
                  aria-hidden="true"
                />
              </button>
            ))}
            <span className="tnum ml-2 text-[15px] font-semibold">{rating}.0</span>
          </div>
        </fieldset>

        <Field
          as="textarea"
          label="리뷰 내용"
          required
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onBlur={() => setTouched(true)}
          error={error}
          hint={`${content.trim().length}/10자 이상`}
          placeholder="착용감, 색감, 마감 상태 등을 적어주시면 다른 분들께 큰 도움이 돼요."
          maxLength={1000}
        />

        <div>
          <p className="mb-2 text-[13px] font-medium">
            사진 첨부 <span className="text-ink-soft">(선택, 최대 {MAX_FILES}장)</span>
          </p>

          <div className="flex flex-wrap gap-2">
            {uploads.map((u) => (
              <div
                key={u.id}
                className="relative size-20 overflow-hidden rounded-sm bg-surface-sunken"
              >
                <div className="absolute inset-x-0 bottom-0 h-1 bg-line">
                  <div
                    className="h-full bg-primary transition-[width] duration-200"
                    style={{ width: `${u.progress}%` }}
                  />
                </div>
                <span className="tnum absolute inset-0 grid place-items-center text-[12px] font-semibold text-ink-soft">
                  {u.progress < 100 ? `${Math.round(u.progress)}%` : '완료'}
                </span>
                <button
                  type="button"
                  onClick={() => setUploads((list) => list.filter((x) => x.id !== u.id))}
                  aria-label={`${u.name} 첨부 취소`}
                  className="absolute top-1 right-1 grid size-6 place-items-center rounded-full bg-ink/70 text-canvas"
                >
                  <X size={12} aria-hidden="true" />
                </button>
              </div>
            ))}

            {uploads.length < MAX_FILES && (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="grid size-20 place-items-center rounded-sm border border-dashed border-line-strong text-ink-soft transition-colors hover:border-ink hover:text-ink"
              >
                <ImagePlus size={20} aria-hidden="true" />
                <span className="sr-only">사진 추가</span>
              </button>
            )}
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            onChange={handleFiles}
            className="sr-only"
            tabIndex={-1}
          />
        </div>
      </form>
    </Sheet>
  );
}
