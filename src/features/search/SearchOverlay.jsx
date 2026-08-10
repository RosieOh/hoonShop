import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Clock, Search, TrendingUp, X } from 'lucide-react';
import Sheet from '@/components/common/Sheet';
import BeadArt from '@/components/common/BeadArt';
import Price from '@/components/common/Price';
import { useDebounce } from '@/hooks/useDebounce';
import { useGetSuggestionsQuery, useGetTrendingQuery } from './searchApi';
import { clearRecent, closeSearch, pushRecent, removeRecent } from './searchSlice';

export default function SearchOverlay() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const open = useSelector((s) => s.search.isOverlayOpen);
  const recent = useSelector((s) => s.search.recent);

  const [keyword, setKeyword] = useState('');
  const inputRef = useRef(null);

  // 타이핑마다 요청하지 않도록 250ms 디바운스
  const debounced = useDebounce(keyword.trim(), 250);

  const { data: suggestions, isFetching } = useGetSuggestionsQuery(debounced, {
    skip: debounced.length < 1,
  });
  const { data: trending } = useGetTrendingQuery(undefined, { skip: !open });

  useEffect(() => {
    if (open) {
      setKeyword('');
      // Sheet가 패널에 포커스를 준 뒤 입력창으로 옮깁니다.
      const id = setTimeout(() => inputRef.current?.focus(), 60);
      return () => clearTimeout(id);
    }
    return undefined;
  }, [open]);

  const submit = (value) => {
    const q = value.trim();
    if (!q) return;
    dispatch(pushRecent(q));
    dispatch(closeSearch());
    navigate(`/products?q=${encodeURIComponent(q)}`);
  };

  const close = () => dispatch(closeSearch());

  return (
    <Sheet open={open} onClose={close} title="검색" side="bottom" className="sm:max-h-[92vh]">
      <div className="px-5 py-5">
        <form
          role="search"
          onSubmit={(e) => {
            e.preventDefault();
            submit(keyword);
          }}
          className="relative"
        >
          <label htmlFor="search-input" className="sr-only">
            상품 검색
          </label>
          <Search
            size={18}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-ink-faint"
            aria-hidden="true"
          />
          <input
            id="search-input"
            ref={inputRef}
            type="search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="어떤 걸 찾고 계세요?"
            autoComplete="off"
            className="h-13 w-full rounded-pill border border-line bg-canvas pr-4 pl-11 text-[16px] outline-none focus:border-primary"
          />
        </form>

        {debounced.length > 0 ? (
          <div className="mt-6 min-h-40">
            {isFetching && <p className="py-6 text-center text-[13px] text-ink-soft">찾는 중…</p>}

            {!isFetching && !suggestions?.products?.length && !suggestions?.keywords?.length && (
              <div className="py-10 text-center">
                <p className="text-[15px] font-medium">‘{debounced}’ 검색 결과가 없어요</p>
                <p className="mt-1.5 text-[13px] text-ink-soft">
                  단어를 줄이거나 카테고리로 둘러보세요
                </p>
              </div>
            )}

            {!!suggestions?.keywords?.length && (
              <ul className="mb-6 flex flex-wrap gap-2">
                {suggestions.keywords.map((k) => (
                  <li key={k}>
                    <button
                      type="button"
                      onClick={() => submit(k)}
                      className="h-9 rounded-pill border border-line px-3.5 text-[13px] hover:border-ink"
                    >
                      {k}
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {!!suggestions?.products?.length && (
              <>
                <p className="eyebrow mb-3">상품</p>
                <ul className="divide-y divide-line">
                  {suggestions.products.map((p) => (
                    <li key={p.id}>
                      <button
                        type="button"
                        onClick={() => {
                          dispatch(closeSearch());
                          navigate(`/products/${p.id}`);
                        }}
                        className="flex w-full items-center gap-3 py-3 text-left transition-colors hover:bg-canvas"
                      >
                        <span className="media-frame size-14 shrink-0 rounded-sm">
                          <BeadArt product={p} decorative className="h-full w-full" />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block truncate text-[14px] font-medium">{p.name}</span>
                          <Price
                            price={p.price}
                            salePrice={p.salePrice}
                            discountRate={p.discountRate}
                            size="sm"
                          />
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        ) : (
          <div className="mt-7 space-y-8">
            <section>
              <div className="mb-3 flex items-center justify-between">
                <p className="eyebrow flex items-center gap-1.5">
                  <Clock size={12} aria-hidden="true" /> 최근 검색어
                </p>
                {recent.length > 0 && (
                  <button
                    type="button"
                    onClick={() => dispatch(clearRecent())}
                    className="text-[12px] text-ink-soft underline underline-offset-4 hover:text-ink"
                  >
                    전체 삭제
                  </button>
                )}
              </div>

              {recent.length === 0 ? (
                <p className="text-[13px] text-ink-faint">최근 검색 기록이 없습니다.</p>
              ) : (
                <ul className="flex flex-wrap gap-2">
                  {recent.map((k) => (
                    <li key={k}>
                      <span className="inline-flex h-9 items-center rounded-pill border border-line pr-1 pl-3.5">
                        <button
                          type="button"
                          onClick={() => submit(k)}
                          className="text-[13px] whitespace-nowrap"
                        >
                          {k}
                        </button>
                        <button
                          type="button"
                          onClick={() => dispatch(removeRecent(k))}
                          aria-label={`최근 검색어 ${k} 삭제`}
                          className="ml-1 flex size-7 items-center justify-center rounded-full text-ink-faint hover:bg-canvas hover:text-ink"
                        >
                          <X size={12} aria-hidden="true" />
                        </button>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section>
              <p className="eyebrow mb-3 flex items-center gap-1.5">
                <TrendingUp size={12} aria-hidden="true" /> 지금 많이 찾는
              </p>
              <ol className="grid gap-x-6 sm:grid-cols-2">
                {(trending?.keywords ?? []).map((k, i) => (
                  <li key={k}>
                    <button
                      type="button"
                      onClick={() => submit(k)}
                      className="flex w-full items-center gap-3 py-2.5 text-left text-[14px] hover:text-primary"
                    >
                      <span className="tnum font-display w-4 text-[16px] font-semibold text-primary">
                        {i + 1}
                      </span>
                      {k}
                    </button>
                  </li>
                ))}
              </ol>
            </section>
          </div>
        )}
      </div>
    </Sheet>
  );
}
