/**
 * CSV export helper shared by the NavigationBar (desktop) and the
 * NavigationDrawer (mobile).
 *
 * Format matches the backend importer:
 * amount;date;description;Recipient name;Recipient address;standard_category name;periodic
 */

import { PaymentItem } from '../types';

const CSV_HEADER_COLUMNS = [
  'amount',
  'date',
  'description',
  'Recipient name',
  'Recipient address',
  'standard_category name',
  'periodic',
];

const escapeCsvField = (rawValue: string): string => {
  const normalized = rawValue.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const shouldQuote = /[";\n]/.test(normalized);
  let escaped = normalized.replace(/"/g, '""');
  if (shouldQuote) {
    escaped = `"${escaped}"`;
  }
  return escaped;
};

const getAuthHeaders = (): HeadersInit => {
  const token =
    localStorage.getItem('financebook_token') ??
    sessionStorage.getItem('financebook_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

/** Fetch all payment items and download them as a semicolon-separated CSV. */
export async function exportPaymentItemsAsCsv(): Promise<void> {
  const response = await fetch('/api/payment-items', { headers: getAuthHeaders() });
  if (!response.ok) {
    throw new Error('Failed to fetch payment items');
  }
  const paymentItems: PaymentItem[] = await response.json();

  const sortedItems = [...paymentItems].sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
  );

  const csvRows = sortedItems.map((item) => {
    const rowValues = [
      item.amount.toString(),
      new Date(item.date).toISOString().split('T')[0],
      item.description || '',
      item.recipient?.name || '',
      item.recipient?.address || '',
      item.standard_category?.name || '',
      item.periodic ? 'true' : 'false',
    ];
    return rowValues.map((value) => escapeCsvField(value ?? '')).join(';');
  });

  const csvContent = [CSV_HEADER_COLUMNS.join(';'), ...csvRows].join('\r\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute(
    'download',
    `payment_items_${new Date().toISOString().split('T')[0]}.csv`,
  );
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
