@param com.github.gcolin.platform.Messages msg
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
@param String selectId
@param Boolean autoSubmit
@param Boolean inline
!{if (selectId == null) selectId = "season_filter";}
!{if (autoSubmit == null) autoSubmit = false;}
!{if (inline == null) inline = false;}
@if(inline)
  <div class="input-group w-auto">
    <label for="${selectId}" class="input-group-text">${msg.get("label.season")}</label>
    @if(autoSubmit)
      <select id="${selectId}" name="seasonId" class="form-select w-auto" onchange="this.form.submit()">
        @for(var seasonOption : seasons)
          @if(seasonOption.isSelected())
            <option value="${seasonOption.getValue()}" selected>${seasonOption.getLabel()}</option>
          @else
            <option value="${seasonOption.getValue()}">${seasonOption.getLabel()}</option>
          @endif
        @endfor
      </select>
    @else
      <select id="${selectId}" name="seasonId" class="form-select w-auto">
        @for(var seasonOption : seasons)
          @if(seasonOption.isSelected())
            <option value="${seasonOption.getValue()}" selected>${seasonOption.getLabel()}</option>
          @else
            <option value="${seasonOption.getValue()}">${seasonOption.getLabel()}</option>
          @endif
        @endfor
      </select>
    @endif
  </div>
@else
<div class="col-12 col-md-auto">
  <label for="${selectId}" class="form-label mb-0">${msg.get("label.season")}</label>
  @if(autoSubmit)
    <select id="${selectId}" name="seasonId" class="form-select" onchange="this.form.submit()">
      @for(var seasonOption : seasons)
        @if(seasonOption.isSelected())
          <option value="${seasonOption.getValue()}" selected>${seasonOption.getLabel()}</option>
        @else
          <option value="${seasonOption.getValue()}">${seasonOption.getLabel()}</option>
        @endif
      @endfor
    </select>
  @else
    <select id="${selectId}" name="seasonId" class="form-select">
      @for(var seasonOption : seasons)
        @if(seasonOption.isSelected())
          <option value="${seasonOption.getValue()}" selected>${seasonOption.getLabel()}</option>
        @else
          <option value="${seasonOption.getValue()}">${seasonOption.getLabel()}</option>
        @endif
      @endfor
    </select>
  @endif
</div>
@endif
