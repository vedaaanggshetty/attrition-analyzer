// A hand-drawn "reviewed" checkbox used to mark a notification as read.
// Adapted from a styled-components reference into plain CSS (scoped via the
// same inline <style> pattern already used elsewhere in this codebase, e.g.
// Dashboard's loader animation) so it doesn't require adding the
// styled-components dependency just for one component.
export function ReviewCheckbox({
  checked,
  onChange,
  disabled = false,
  label = "Reviewed",
}: {
  checked: boolean;
  onChange: () => void;
  disabled?: boolean;
  label?: string;
}) {
  return (
    <label className={`review-checkbox ${disabled ? "is-disabled" : ""}`}>
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={onChange}
        aria-label={label}
      />
      <span className="checkmark" />
      <span className="review-text">
        <span className="review-text-content">{label}</span>
        <svg preserveAspectRatio="none" viewBox="0 0 400 20" className="cut-line" aria-hidden="true">
          <path d="M0,10 H400" />
        </svg>
      </span>

      <svg height={0} width={0}>
        <filter id="reviewHandDrawnNoise">
          <feTurbulence result="noise" numOctaves={8} baseFrequency="0.1" type="fractalNoise" />
          <feDisplacementMap yChannelSelector="G" xChannelSelector="R" scale={2} in2="noise" in="SourceGraphic" />
        </filter>
      </svg>

      <style>{`
        .review-checkbox {
          position: relative;
          display: inline-flex;
          align-items: center;
          gap: 0.5em;
          cursor: pointer;
          user-select: none;
          font-size: 0.8rem;
          font-weight: 600;
          padding-left: 1.6em;
        }
        .review-checkbox.is-disabled { cursor: not-allowed; opacity: 0.6; }
        .review-checkbox input {
          position: absolute;
          opacity: 0;
          cursor: pointer;
        }
        .review-checkbox .checkmark {
          position: absolute;
          left: 0;
          top: 0.05em;
          height: 1.05em;
          width: 1.05em;
          border-radius: 6px;
          border: 2px solid rgba(59, 59, 66, 0.35);
          filter: url(#reviewHandDrawnNoise);
          background: radial-gradient(rgba(59, 59, 66, 0) 65%, rgba(59, 59, 66, 0) 70%);
          transition: border-color 0.2s ease;
        }
        .review-checkbox input:checked ~ .checkmark {
          border-color: rgba(34, 139, 87, 0.55);
          filter: url(#reviewHandDrawnNoise);
          animation: reviewMark 0.3s forwards;
        }
        @keyframes reviewMark {
          0%   { background: radial-gradient(rgba(34,139,87,0.24) 0%, rgba(34,139,87,0) 35%); }
          50%  { background: radial-gradient(rgba(34,139,87,0.24) 0%, rgba(34,139,87,0) 35%); }
          75%  { background: radial-gradient(rgba(34,139,87,0.95) 35%, rgba(34,139,87,0) 60%); }
          100% { background: radial-gradient(rgba(34,139,87,0.95) 65%, rgba(34,139,87,0) 70%); }
        }
        .review-checkbox .review-text {
          position: relative;
          display: inline-block;
          color: rgb(82, 82, 91);
          transition: color 0.3s ease;
        }
        .review-checkbox .cut-line {
          position: absolute;
          left: 0;
          right: 0;
          bottom: 30%;
          height: 1em;
          width: calc(100% + 8px);
          pointer-events: none;
          stroke: rgb(34, 139, 87);
          stroke-width: 1.5;
          fill: none;
          transform: scaleX(0);
          transform-origin: left;
          transition: transform 0.3s ease;
        }
        .review-checkbox input:checked ~ .review-text .cut-line { transform: scaleX(1); }
        .review-checkbox input:checked ~ .review-text { color: rgba(82, 82, 91, 0.55); }
      `}</style>
    </label>
  );
}
