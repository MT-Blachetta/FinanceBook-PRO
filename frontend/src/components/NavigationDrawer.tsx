/**
 * Slide-in navigation drawer for small screens.
 *
 * Contains everything that is hidden from the top bar on narrow viewports:
 * page links, the view filters (All / Expenses / Incomes / Fees),
 * CSV import/export, and logout.
 *
 * Accessibility: closes on overlay click and Escape, traps nothing heavy —
 * it is a plain complementary navigation region.
 */

import React, { useEffect, useRef } from 'react';
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';
import clsx from 'clsx';

import { ViewFilter } from './NavigationBar';
import { useImportCSV } from '../api/hooks';
import { exportPaymentItemsAsCsv } from '../utils/csvExport';

/* ── Styled Components ───────────────────────────────────────────── */

const Overlay = styled.div<{ $open: boolean }>`
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  opacity: ${({ $open }) => ($open ? 1 : 0)};
  pointer-events: ${({ $open }) => ($open ? 'auto' : 'none')};
  transition: opacity 0.25s ease;
  z-index: 999;
`;

const Panel = styled.nav<{ $open: boolean }>`
  position: fixed;
  top: 0;
  left: 0;
  height: 100dvh;
  width: min(80vw, 300px);
  background: var(--color-surface);
  border-right: 1px solid #333;
  padding: var(--spacing-md);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  transform: translateX(${({ $open }) => ($open ? '0' : '-105%')});
  transition: transform 0.25s ease;
  z-index: 1000;
  overflow-y: auto;
`;

const DrawerHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  button {
    background: none;
    border: none;
    color: var(--color-text-secondary);
    font-size: 1.5rem;
    line-height: 1;
    cursor: pointer;
    padding: 0.25rem 0.5rem;

    &:hover {
      color: var(--color-text-primary);
    }
  }
`;

const SectionTitle = styled.h3`
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #777;
  margin-top: var(--spacing-sm);
`;

const LinkList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
`;

const DrawerButton = styled.button`
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: 0.95rem;
  padding: 0.6rem 0.75rem;
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:hover {
    background: #333;
  }

  &.active {
    background: #333;
    font-weight: 600;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const LogoutButton = styled(DrawerButton)`
  margin-top: auto;
  background: #2563eb;
  color: white;
  text-align: center;
  font-weight: 600;

  &:hover {
    background: #1d4ed8;
  }
`;

/* ── Component ───────────────────────────────────────────────────── */

interface NavigationDrawerProps {
  open: boolean;
  activeFilter: ViewFilter;
  onClose(): void;
  onFilterChange(filter: ViewFilter): void;
  onLogout(): void;
}

const FILTER_LABELS: Record<ViewFilter, string> = {
  all: 'All',
  expenses: 'Expenses',
  incomes: 'Incomes',
  fees: 'Fees',
};

export const NavigationDrawer: React.FC<NavigationDrawerProps> = ({
  open,
  activeFilter,
  onClose,
  onFilterChange,
  onLogout,
}) => {
  const navigate = useNavigate();
  const importCsvMutation = useImportCSV();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  const go = (path: string) => {
    navigate(path);
    onClose();
  };

  const handleExport = async () => {
    try {
      await exportPaymentItemsAsCsv();
    } catch (error) {
      console.error('Error exporting CSV:', error);
      alert('Failed to export CSV. Please try again.');
    }
    onClose();
  };

  const handleImportFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    try {
      const result = await importCsvMutation.mutateAsync(file);
      alert(
        `CSV import completed. Created ${result.created_payments} payments, ` +
          `${result.created_recipients} recipients, updated ${result.updated_recipients} recipients, ` +
          `and created ${result.created_categories} categories.`,
      );
    } catch (error) {
      console.error('Error importing CSV:', error);
      alert('Failed to import CSV. Please ensure the file matches the exported format.');
    } finally {
      event.target.value = '';
      onClose();
    }
  };

  return (
    <>
      <Overlay $open={open} onClick={onClose} aria-hidden={!open} />
      <Panel $open={open} aria-label="Main navigation" aria-hidden={!open}>
        <DrawerHeader>
          <h2>FinanceBook</h2>
          <button onClick={onClose} aria-label="Close menu">
            ×
          </button>
        </DrawerHeader>

        <SectionTitle>Pages</SectionTitle>
        <LinkList>
          <li><DrawerButton onClick={() => go('/')}>Summary</DrawerButton></li>
          <li><DrawerButton onClick={() => go('/add')}>Add Payment</DrawerButton></li>
          <li><DrawerButton onClick={() => go('/statistics')}>Statistics</DrawerButton></li>
          <li><DrawerButton onClick={() => go('/categories')}>Categories</DrawerButton></li>
          <li><DrawerButton onClick={() => go('/category-types')}>Category Types</DrawerButton></li>
        </LinkList>

        <SectionTitle>View</SectionTitle>
        <LinkList>
          {(Object.keys(FILTER_LABELS) as ViewFilter[]).map((filter) => (
            <li key={filter}>
              <DrawerButton
                className={clsx({ active: activeFilter === filter })}
                onClick={() => onFilterChange(filter)}
              >
                {FILTER_LABELS[filter]}
              </DrawerButton>
            </li>
          ))}
        </LinkList>

        <SectionTitle>Data</SectionTitle>
        <LinkList>
          <li>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              style={{ display: 'none' }}
              onChange={handleImportFileChange}
            />
            <DrawerButton
              onClick={() => fileInputRef.current?.click()}
              disabled={importCsvMutation.isPending}
            >
              {importCsvMutation.isPending ? 'Importing…' : 'Import CSV'}
            </DrawerButton>
          </li>
          <li><DrawerButton onClick={handleExport}>Export CSV</DrawerButton></li>
        </LinkList>

        <LogoutButton onClick={onLogout}>Logout</LogoutButton>
      </Panel>
    </>
  );
};
