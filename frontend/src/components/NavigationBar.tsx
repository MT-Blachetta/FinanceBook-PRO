/**
 * Persistent top-navigation bar.
 *
 * Responsibilities:
 * - Displays application title
 * - Provides quick filters (All, Expenses, Incomes, Fees)
 * - Page links (Categories, Category Types, Statistics) on wide screens
 * - CSV import/export on wide screens
 * - Hamburger menu on small screens that opens the NavigationDrawer,
 *   which contains everything hidden here
 *
 * Responsive behaviour (no information is ever unreachable):
 * - ≤ 900px: page links and CSV buttons move into the drawer
 * - ≤ 760px: the filter row wraps onto its own full-width line
 * - ≤ 560px: filters move into the drawer as well; hamburger stays
 *
 * The component is presentational; it receives the active filter and
 * callbacks via props.
 */

import React from 'react';
import styled from 'styled-components';
import clsx from 'clsx';
import { useNavigate } from 'react-router-dom';
import { useImportCSV } from '../api/hooks';
import { exportPaymentItemsAsCsv } from '../utils/csvExport';

/* Types & Props */

export type ViewFilter = 'all' | 'expenses' | 'incomes' | 'fees';

interface NavigationBarProps {
  active: ViewFilter;
  onChange(filter: ViewFilter): void;

  /** Callback when user clicks the hamburger icon. */
  onMenu(): void;

  /** Optional callback when user clicks the ADD button. */
  onAdd?(): void;

  /** Callback when user clicks the Logout button. */
  onLogout(): void;
}

/* Styled Components */

const Bar = styled.header`
  background: var(--color-surface);
  padding: var(--spacing-sm) clamp(0.5rem, 2vw, 1rem);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  border-bottom: 1px solid #272727;
  position: sticky;
  top: 0;
  z-index: 100;
`;

const LeftSection = styled.div`
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  min-width: 0;
`;

const Title = styled.h1`
  font-size: clamp(1.05rem, 2.5vw, 1.25rem);
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  white-space: nowrap;
`;

const PageLinks = styled.div`
  display: flex;
  gap: var(--spacing-sm);

  button {
    background: none;
    border: 1px solid #444;
    color: var(--color-text-secondary);
    font-size: 0.9rem;
    cursor: pointer;
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: var(--radius-md);
    transition: all 0.2s ease;
    white-space: nowrap;

    &:hover {
      background: #333;
      color: var(--color-text-primary);
      border-color: #555;
    }
  }

  /* Links live in the drawer on smaller screens */
  @media (max-width: 900px) {
    display: none;
  }
`;

const Filters = styled.nav`
  display: flex;
  gap: var(--spacing-sm);
  flex: 1 1 auto;
  justify-content: center;
  min-width: 0;

  button {
    background: none;
    border: none;
    color: var(--color-text-secondary);
    font-size: 0.9rem;
    cursor: pointer;
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: var(--radius-md);
    white-space: nowrap;

    &.active {
      background: #333;
      color: var(--color-text-primary);
    }

    &:hover {
      color: var(--color-text-primary);
    }
  }

  /* Wrap onto an own full-width row on narrow screens */
  @media (max-width: 760px) {
    order: 4;
    width: 100%;
    flex-basis: 100%;
    justify-content: center;
    border-top: 1px solid #272727;
    padding-top: var(--spacing-xs);
  }

  /* Very small screens: filters are reachable via the drawer */
  @media (max-width: 560px) {
    display: none;
  }
`;

const CSVButtons = styled.div`
  display: flex;
  gap: var(--spacing-sm);

  button {
    background: none;
    border: 1px solid #444;
    color: var(--color-text-secondary);
    font-size: 0.9rem;
    cursor: pointer;
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: var(--radius-md);
    transition: all 0.2s ease;
    white-space: nowrap;

    &:hover {
      background: #333;
      color: var(--color-text-primary);
      border-color: #555;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  /* CSV actions live in the drawer on smaller screens */
  @media (max-width: 900px) {
    display: none;
  }
`;

const AddButton = styled.button`
  background: var(--color-positive);
  color: white;
  border: none;
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
  white-space: nowrap;

  &:hover {
    background: #059669;
  }

  @media (max-width: 640px) {
    padding: var(--spacing-xs) var(--spacing-sm);
    font-size: 0.8rem;
  }
`;

const MenuButton = styled.button`
  display: none; /* hidden on desktop */
  background: transparent;
  border: none;
  padding: var(--spacing-xs);
  cursor: pointer;
  align-items: center;
  justify-content: center;

  svg {
    width: 24px;
    height: 24px;
    fill: var(--color-text-primary);
  }

  &:hover svg {
    fill: var(--color-positive);
  }

  @media (max-width: 900px) {
    display: flex;
  }
`;

const RightSection = styled.div`
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: var(--spacing-sm);
`;

const LogoutButton = styled.button`
  background: #2563eb;
  color: white;
  border: none;
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
  white-space: nowrap;

  &:hover {
    background: #1d4ed8;
  }

  @media (max-width: 640px) {
    padding: var(--spacing-xs) var(--spacing-sm);
    font-size: 0.8rem;
  }

  /* On the smallest screens logout is available inside the drawer */
  @media (max-width: 400px) {
    display: none;
  }
`;

/** Simple hamburger icon (three bars). */
const MenuIcon: React.FC = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
    <rect x="3" y="5" width="18" height="2" rx="1" />
    <rect x="3" y="11" width="18" height="2" rx="1" />
    <rect x="3" y="17" width="18" height="2" rx="1" />
  </svg>
);

/* Component */

export const NavigationBar: React.FC<NavigationBarProps> = ({
  active,
  onChange,
  onMenu,
  onAdd,
  onLogout,
}) => {
  const navigate = useNavigate();
  const importCsvMutation = useImportCSV();
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const handleExportCSV = async () => {
    try {
      await exportPaymentItemsAsCsv();
    } catch (error) {
      console.error('Error exporting CSV:', error);
      alert('Failed to export CSV. Please try again.');
    }
  };

  const handleImportCSVClick = () => {
    fileInputRef.current?.click();
  };

  const handleImportFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    if (!file.name.toLowerCase().endsWith('.csv')) {
      alert('Please select a CSV file that was exported from FinanceBook.');
      event.target.value = '';
      return;
    }

    try {
      const result = await importCsvMutation.mutateAsync(file);
      alert(
        `CSV import completed. Created ${result.created_payments} payments, ` +
          `${result.created_recipients} recipients, updated ${result.updated_recipients} recipients, ` +
          `and created ${result.created_categories} categories.`,
      );
    } catch (error: unknown) {
      console.error('Error importing CSV:', error);
      let message = 'Failed to import CSV. Please ensure the file matches the exported format.';
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { data?: { detail?: string } } };
        const detail = axiosError.response?.data?.detail;
        if (typeof detail === 'string' && detail.trim().length > 0) {
          message = detail;
        }
      } else if (error instanceof Error && error.message) {
        message = error.message;
      }
      alert(message);
    } finally {
      event.target.value = '';
    }
  };

  return (
    <Bar>
      <MenuButton aria-label="Open menu" onClick={onMenu}>
        <MenuIcon />
      </MenuButton>

      <LeftSection>
        <Title>FinanceBook</Title>
        <PageLinks>
          <button onClick={() => navigate('/categories')}>Categories</button>
          <button onClick={() => navigate('/category-types')}>Category Types</button>
          <button onClick={() => navigate('/statistics')}>Statistics</button>
        </PageLinks>
      </LeftSection>

      <Filters aria-label="View filter">
        <button
          className={clsx({ active: active === 'all' })}
          onClick={() => onChange('all')}
        >
          All
        </button>
        <button
          className={clsx({ active: active === 'expenses' })}
          onClick={() => onChange('expenses')}
        >
          Expenses
        </button>
        <button
          className={clsx({ active: active === 'incomes' })}
          onClick={() => onChange('incomes')}
        >
          Incomes
        </button>
        <button
          className={clsx({ active: active === 'fees' })}
          onClick={() => onChange('fees')}
        >
          Fees
        </button>
      </Filters>

      <RightSection>
        <CSVButtons>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,text/csv"
            style={{ display: 'none' }}
            onChange={handleImportFileChange}
          />
          <button
            type="button"
            onClick={handleImportCSVClick}
            disabled={importCsvMutation.isPending}
          >
            {importCsvMutation.isPending ? 'importing…' : 'import CSV'}
          </button>
          <button type="button" onClick={handleExportCSV}>export CSV</button>
        </CSVButtons>
        {onAdd && <AddButton onClick={onAdd}>ADD</AddButton>}
        <LogoutButton onClick={onLogout}>Logout</LogoutButton>
      </RightSection>
    </Bar>
  );
};
