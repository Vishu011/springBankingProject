// src/app/features/cards/transaction-limit-modal/transaction-limit-modal.component.ts

import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardResponse } from '../../../shared/models/card.model'; // Import CardResponse

@Component({
  selector: 'app-transaction-limit-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-limit-modal.component.html',
  styleUrls: ['./transaction-limit-modal.component.css']
})
export class TransactionLimitModalComponent implements OnInit {
  @Input() card!: CardResponse; // Input: The card object to display
  @Output() closeModal = new EventEmitter<void>(); // Output: Event to close modal

  errorMessage: string | null = null;

  ngOnInit(): void {
    // No-op: This modal is informational only for now (no limit update in current backend)
  }

  // Close modal
  onCloseModal(): void {
    this.errorMessage = null;
    this.closeModal.emit();
  }
}
