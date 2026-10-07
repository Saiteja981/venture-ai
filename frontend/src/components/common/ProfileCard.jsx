import Card from "./Card.jsx";

export default function ProfileCard({ title, children, action }) {
    return (
        <Card>
            <div className="flex items-center justify-between">
                <h3 className="text-base font-semibold text-navy-800">{title}</h3>
                {action}
            </div>
            <div className="mt-4">{children}</div>
        </Card>
    );
}