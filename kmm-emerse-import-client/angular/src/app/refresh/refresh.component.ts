import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {noop} from 'rxjs';
import {ConfigService} from '../config/config.service';

@Component({
    selector: 'emerse-refresh',
    templateUrl: './refresh.component.html',
    styleUrls: ['./refresh.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class RefreshComponent {

    private refreshHandle: number;

    private refreshInterval: number;

    @Input()
    set autoRefresh(value: boolean) {
        this._autoRefresh(value);
    }

    get autoRefresh(): boolean {
        return this.refreshHandle != null;
    }

    @Input()
    busy: boolean;

    @Output() onRefresh = new EventEmitter<Boolean>();

    constructor(configService: ConfigService) {
        configService.getSettingAsync("app.refresh.seconds").subscribe(interval => {
            this.refreshInterval = Number(interval);
            this.refreshInterval = isNaN(this.refreshInterval) ? 10
                : this.refreshInterval < 5 ? 5 : this.refreshInterval;
        });
    }

    refresh(reset = true): void {
        this.onRefresh.emit(true);
        reset ? this.resetAutoRefresh() : noop();
    }

    private _autoRefresh(value: boolean): void {
        const previous = this.refreshHandle != null;

        if (value !== previous) {
            if (value) {
                this.refreshHandle = setInterval(() => this.refresh(false), this.refreshInterval * 1000);
            } else {
                clearInterval(this.refreshHandle);
                this.refreshHandle = null;
            }
        }
    }

    resetAutoRefresh(): void {
        const previous: boolean = this.autoRefresh;
        this.autoRefresh = false;
        this.autoRefresh = previous;
    }
}
