export default function Card({ children, className = "", hover = false, as: Tag = "div", ...rest }) {
    return (
        <Tag
            className={`card ${hover ? "card-hover" : ""} ${className}`}
            {...rest}
        >
            {children}
        </Tag>
    );
}