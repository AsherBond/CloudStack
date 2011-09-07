(function($, cloudStack) {
  /**
   * Instance wizard
   */
  cloudStack.instanceWizard = function(args) {
    return function(listViewArgs) {
      var $wizard = $('#template').find('div.instance-wizard').clone();
      var $progress = $wizard.find('div.progress ul li');
      var $steps = $wizard.find('div.steps').children().hide();
      var $diagramParts = $wizard.find('div.diagram').children().hide();
      var $form = $wizard.find('form')

      $form.validate();

      // Close instance wizard
      var close = function() {
        $wizard.dialog('destroy');
        $('div.overlay').fadeOut(function() { $('div.overlay').remove(); });
      };

      // Save instance and close wizard
      var completeAction = function() {
        args.complete({
          // Populate data
          data: cloudStack.serializeForm($form),
          response: {
            success: function(args) {
              listViewArgs.complete({
                messageArgs: cloudStack.serializeForm($form)
              });
              close();
            }
          }
        });
      };

      // Go to specified step in wizard,
      // updating nav items and diagram
      var showStep = function(index) {
        var targetIndex = index - 1;

        if (index <= 1) targetIndex = 0;
        if (targetIndex == $steps.size()) {
          completeAction();
        }

        var $targetStep = $($steps.hide()[targetIndex]).show();

        // Show launch vm button if last step
        var $nextButton = $wizard.find('.button.next');
        $nextButton.find('span').html('Next');
        $nextButton.removeClass('final');
        if ($targetStep.hasClass('review')) {
          $nextButton.find('span').html('Launch VM');
          $nextButton.addClass('final');
        }

        // Show relevant conditional sub-step if present
        if ($targetStep.has('.wizard-step-conditional')) {
          $targetStep.find('.wizard-step-conditional').hide();
          $targetStep.find('.wizard-step-conditional.select-network').show();
        }

        // Update progress bar
        var $targetProgress = $progress.removeClass('active').filter(function() {
          return $(this).index() <= targetIndex;
        }).toggleClass('active');

        // Update diagram; show/hide as necessary
        $diagramParts.filter(function() {
          return $(this).index() <= targetIndex;
        }).fadeIn('slow');
        $diagramParts.filter(function() {
          return $(this).index() > targetIndex;
        }).fadeOut('slow');


        setTimeout(function() {
          if (!$targetStep.find('input[type=radio]:checked').size()) {
            $targetStep.find('input[type=radio]:first').click();
          }
        }, 50);
      };

      // Events
      $wizard.click(function(event) {
        var $target = $(event.target);

        // Next button
        if ($target.closest('div.button.next').size()) {
          if (!$form.valid()) return false;

          showStep($steps.filter(':visible').index() + 2);

          return false;
        }

        // Previous button
        if ($target.closest('div.button.previous').size()) {
          showStep($steps.filter(':visible').index());

          return false;
        }

        // Close button
        if ($target.closest('div.button.cancel').size()) {
          close();
          
          return false;
        }

        // Edit link
        if ($target.closest('div.edit').size()) {
          var $edit = $target.closest('div.edit');

          showStep($edit.find('a').attr('href'));

          return false;
        }

        return true;
      });

      showStep(1);

      // Setup tabs and slider
      $wizard.find('.tab-view').tabs();
      $wizard.find('.slider').slider({
        min: 1,
        max: 100,
        start: function(event) {
          $wizard.find('div.data-disk-offering div.custom-size input[type=radio]').click();
        },
        slide: function(event, ui) {
          $wizard.find('div.data-disk-offering div.custom-size input[type=text]').val(
            ui.value
          );
        }
      });

      return $wizard.dialog({
        title: 'Add instance',
        width: 800,
        height: 570,
        zIndex: 5000
      })
        .closest('.ui-dialog').overlay();
    };
  };  
})(jQuery, cloudStack);